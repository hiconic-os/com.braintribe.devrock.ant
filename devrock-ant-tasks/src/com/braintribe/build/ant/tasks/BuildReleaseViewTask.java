// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newTreeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
import com.braintribe.common.lcd.Pair;
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
import com.braintribe.model.version.Version;
import com.braintribe.model.version.VersionExpression;
import com.braintribe.model.version.VersionRange;
import com.braintribe.utils.FileTools;

import devrock.releasing.model.configuration.ReleaseConfiguration;

/**
 * Computes a {@link RepositoryView} based on {@link ReleaseConfiguration} (see for more details on releasing).
 * 
 * @author peter.gazdik
 */
public class BuildReleaseViewTask extends Task {
	private static final VersionRange fullRange = VersionRange.from(null, false, null, false);
	private String displayName;
	private File configuration;
	private File outputFile;
	private String uploadRepo;

	@Configurable
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	@Configurable
	public void setUploadRepo(String uploadRepo) {
		this.uploadRepo = uploadRepo;
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

	private static class ArtifactCondition {
		public final Pattern identificationPattern;
		public final VersionExpression versionPattern;
		public ArtifactCondition(Pattern identificationPattern, VersionExpression versionPattern) {
			super();
			this.identificationPattern = identificationPattern;
			this.versionPattern = versionPattern;
		}
	}
	 
	
	private class BuildReleaseViewExecution {
		

		private final ReleaseConfiguration releaseConfiguration;
		private final List<ArtifactCondition> includes;
		private final List<ArtifactCondition> excludes;

		public BuildReleaseViewExecution() {
			releaseConfiguration = YamlConfigurations.read(ReleaseConfiguration.T).from(configuration).get();
			includes = parsePatterns(releaseConfiguration.getIncludes(), true);
			excludes = parsePatterns(releaseConfiguration.getExcludes(), false);
		}

		private List<ArtifactCondition> parsePatterns(List<String> simpleExpressions, boolean ensureNotEmpty) {
			List<ArtifactCondition> patterns = new ArrayList<>();
			for (String expr: simpleExpressions) {
				int index = expr.indexOf('#');
				
				if (index == -1) {
					Pattern pattern = toPattern(expr);
					patterns.add(new ArtifactCondition(pattern, fullRange));
				}
				else {
					Pattern pattern = toPattern(expr.substring(0, index));
					VersionExpression versionExpression = VersionExpression.parse(expr.substring(index + 1));
					patterns.add(new ArtifactCondition(pattern, versionExpression));
				}
			}
			
			if (ensureNotEmpty && patterns.isEmpty()) {
				patterns.add(new ArtifactCondition(Pattern.compile(".*"), fullRange));
			}
			
			return patterns;
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

			Collection<Pair<String, Version>> artifacts = resolveArtifactsToRelease(artifactIndex);

			ArtifactFilter filter = buildArtifactFilter(artifacts);

			RepositoryView repoView = createRepositoryView(filter);

			writeOutput(repoView);
		}

		private ArtifactIndex readArtifactIndex() {
			McBridge bridge = Bridges.getInstance(getProject());

			RepositoryConfiguration repositoryConfiguration = bridge.getRepositoryConfiguration();

			Repository uploadRepository = extracted(repositoryConfiguration);
			if (uploadRepository == null)
				throw new BuildException("Cannot build release view. No upload repository configured to read the artifact index from."
						+ optionalOrigination(repositoryConfiguration));

			Maybe<ArtifactIndex> maybeArtifactIndex = bridge.readLatestArtifactIndex(uploadRepository);
			if (maybeArtifactIndex.isUnsatisfied())
				throw new BuildException("Cannot build release view. " + maybeArtifactIndex.whyUnsatisfied().stringify());

			return maybeArtifactIndex.get();
		}

		private Repository extracted(RepositoryConfiguration repositoryConfiguration) {
			if (uploadRepo == null)
				return repositoryConfiguration.getUploadRepository();
			
			return repositoryConfiguration.getRepositories().stream() //
					.filter(r -> uploadRepo.equals(r.getName())) //
					.findFirst() //
					.orElse(null);
		}

		private String optionalOrigination(RepositoryConfiguration repositoryConfiguration) {
			if (repositoryConfiguration == null)
				return "";
			else
				return " Repo config origination: " + repositoryConfiguration.getOrigination();
		}

		private Collection<Pair<String, Version>> resolveArtifactsToRelease(ArtifactIndex artifactIndex) {
			
			Map<Pair<ArtifactCondition, String>, Version> matches = newTreeMap();
			
			for (String artifactAsString : artifactIndex.getArtifacts()) {
				CompiledArtifactIdentification cai = CompiledArtifactIdentification.parse(artifactAsString);
				
				Version version = cai.getVersion();
				String versionlessName = ArtifactIdentification.from(cai).asString();
				
				if (matches(excludes, versionlessName, version) != null)
					continue;
				
				ArtifactCondition includeCondition = matches(includes, versionlessName, version);
				if (includeCondition == null) 
					continue;
					
				Pair<ArtifactCondition, String> key = Pair.of(includeCondition, versionlessName);
				
				matches.compute(key, (k, v) -> version.isHigherThan(v) ? version : v);
			}
			
			List<Pair<String, Version>> artifacts = new ArrayList<>();
			
			for (Map.Entry<Pair<ArtifactCondition, String>, Version> match: matches.entrySet()) {
				artifacts.add(Pair.of(match.getKey().second(), match.getValue()));
			}

			return artifacts;
		}
		
		private ArtifactCondition matches(List<ArtifactCondition> conditions, String versionlessName, Version version) {
			for (ArtifactCondition condition: conditions) {
				if (condition.identificationPattern.matcher(versionlessName).matches() && condition.versionPattern.matches(version))
					return condition;
			}
			
			return null;
		}

		private ArtifactFilter buildArtifactFilter(Collection<Pair<String, Version>> artifacts) {
			LockArtifactFilter result = LockArtifactFilter.T.create();

			Set<String> locks = result.getLocks();
			
			Comparator<CompiledArtifactIdentification> comparator = Comparator.comparing(CompiledArtifactIdentification::getGroupId) //
					.thenComparing(CompiledArtifactIdentification::getArtifactId) //
					.thenComparing(CompiledArtifactIdentification::getVersion);
			
			var sortedArtifacts = artifacts.stream() //
					.map(e -> CompiledArtifactIdentification.from(ArtifactIdentification.parse(e.first()), e.second())) //
					.sorted(comparator) //
					.toList();

			for (var artifact: sortedArtifacts)
				locks.add(artifact.asString());

			return result;
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
