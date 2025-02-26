// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newTreeMap;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.ant.utils.DrAntTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.devrock.mc.core.declared.commons.HashComparators;
import com.braintribe.devrock.mc.core.repository.index.ArtifactIndex;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.devrock.model.repository.filters.ArtifactFilter;
import com.braintribe.devrock.model.repository.filters.LockArtifactFilter;
import com.braintribe.devrock.model.repositoryview.RepositoryView;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.version.Version;
import com.braintribe.utils.FileTools;

import devrock.releasing.model.configuration.ReleaseConfiguration;

/**
 * Computes a {@link RepositoryView} based on {@link ReleaseConfiguration} (see for more details on releasing).
 * 
 * @author peter.gazdik
 */
public class BuildReleaseViewTask extends Task {

	private String displayName;
	private File configuration;
	private File outputFile;

	@Configurable
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Required
	public void setConfiguration(File configuration) {
		this.configuration = configuration;
	}

	@Required
	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public void execute() throws BuildException {
		DrAntTools.runAndPrintStacktraceIfNonBuildException(this::_execute);
	}

	private void _execute() throws BuildException {
		new BuildReleaseViewExecution().run();
	}

	private class BuildReleaseViewExecution {

		private final ReleaseConfiguration releaseConfiguration;
		private final List<Pattern> includes;
		private final List<Pattern> excludes;

		public BuildReleaseViewExecution() {
			releaseConfiguration = YamlConfigurations.read(ReleaseConfiguration.T).from(configuration).get();
			includes = toPatterns(releaseConfiguration.getIncludes());
			excludes = toPatterns(releaseConfiguration.getExcludes());
		}

		private List<Pattern> toPatterns(List<String> simpleExpressions) {
			return simpleExpressions.stream() //
					.map(this::toPattern) //
					.collect(Collectors.toList());
		}

		private Pattern toPattern(String simpleExpression) {
			String regEx = simpleExpression //
					.replace(".", "\\.") //
					.replace("?", ".") //
					.replace("*", ".*");

			return Pattern.compile(regEx);
		}

		public void run() {
			ArtifactIndex artifactIndex = readArtifactIndex();

			Map<String, Version> artifacts = resolveArtifactsToRelease(artifactIndex);

			ArtifactFilter filter = buildArtifactFilter(artifacts);

			RepositoryView repoView = createRepositoryView(filter);

			writeOutput(repoView);
		}

		private ArtifactIndex readArtifactIndex() {
			McBridge bridge = Bridges.getInstance(getProject());

			RepositoryConfiguration repositoryConfiguration = bridge.getRepositoryConfiguration();

			Repository uploadRepository = repositoryConfiguration.getUploadRepository();
			if (uploadRepository == null)
				throw new BuildException("Cannot build release view. No upload repository configured to read the artifact index from."
						+ optionalOrigination(repositoryConfiguration));

			Maybe<ArtifactIndex> maybeArtifactIndex = bridge.readLatestArtifactIndex(uploadRepository);
			if (maybeArtifactIndex.isUnsatisfied())
				throw new BuildException("Cannot build release view. " + maybeArtifactIndex.whyUnsatisfied().stringify());

			return maybeArtifactIndex.get();
		}

		private String optionalOrigination(RepositoryConfiguration repositoryConfiguration) {
			if (repositoryConfiguration == null)
				return "";
			else
				return " Repo config origination: " + repositoryConfiguration.getOrigination();
		}

		private Map<String, Version> resolveArtifactsToRelease(ArtifactIndex artifactIndex) {
			Map<String, Version> artifacts = newTreeMap();
			
			for (String artifactAsString : artifactIndex.getArtifacts()) {
				CompiledArtifactIdentification cai = CompiledArtifactIdentification.parse(artifactAsString);
				Version version = cai.getVersion();

				String versionlessName = ArtifactIdentification.from(cai).asString();
				artifacts.compute(versionlessName, (k, v) -> version.isHigherThan(v) ? version : v);
			}

			artifacts.keySet().removeIf(this::isNotIncluded);

			return artifacts;
		}

		private ArtifactFilter buildArtifactFilter(Map<String, Version> artifacts) {
			LockArtifactFilter result = LockArtifactFilter.T.create();

			Set<String> locks = result.getLocks();
			
			Comparator<CompiledArtifactIdentification> comparator = Comparator.comparing(CompiledArtifactIdentification::getGroupId) //
					.thenComparing(CompiledArtifactIdentification::getArtifactId) //
					.thenComparing(CompiledArtifactIdentification::getVersion);
			
			var sortedArtifacts = artifacts.entrySet().stream() //
					.map(e -> CompiledArtifactIdentification.from(ArtifactIdentification.parse(e.getKey()), e.getValue())) //
					.sorted(comparator) //
					.toList();

			for (var artifact: sortedArtifacts)
				locks.add(artifact.asString());

			return result;
		}

		private boolean isNotIncluded(String versionlessName) {
			return !isIncluded(versionlessName);
		}

		private boolean isIncluded(String versionlessName) {
			return matchesAny(versionlessName, includes) && //
					!matchesAny(versionlessName, excludes);
		}

		private boolean matchesAny(String versionlessName, List<Pattern> patterns) {
			for (Pattern p : patterns)
				if (p.matcher(versionlessName).matches())
					return true;

			return false;
		}

		private RepositoryView createRepositoryView(ArtifactFilter filter) {
			Repository repo = releaseConfiguration.getRepositoryPrototype();
			if (repo == null)
				throw new BuildException("Cannot build release view, repositoryPrototype was not configured in " + configuration.getAbsolutePath());

			repo.setArtifactFilter(filter);

			RepositoryView repoView = RepositoryView.T.create();
			if (displayName != null)
				repoView.setDisplayName(displayName);
			
			repoView.getRepositories().add(repo);

			return repoView;
		}

		private void writeOutput(RepositoryView repoView) {
			FileTools.write(outputFile).usingOutputStream( //
					os -> new YamlMarshaller().marshall(os, repoView, GmSerializationOptions.defaultOptions.derive() //
							.set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic) //
							.writeEmptyProperties(false) //
							.writeAbsenceInformation(false) //
							.build() //
					));
		}

	}

}
