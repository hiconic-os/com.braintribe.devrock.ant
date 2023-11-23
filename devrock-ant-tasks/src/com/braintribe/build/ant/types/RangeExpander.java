// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.types;

import static com.braintribe.utils.lcd.CollectionTools2.asList;
import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.first;
import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.braintribe.artifact.declared.marshaller.DeclaredArtifactMarshaller;
import com.braintribe.build.ant.tasks.EnsureRange;
import com.braintribe.build.ant.utils.ArtifactGroup;
import com.braintribe.cc.lcd.EqProxy;
import com.braintribe.common.RegexCheck;
import com.braintribe.devrock.mc.core.declared.DeclaredArtifactIdentificationExtractor;
import com.braintribe.devrock.mc.core.declared.commons.HashComparators;
import com.braintribe.devrock.model.mc.reason.DeclaredArtifactReadError;
import com.braintribe.devrock.model.mc.reason.MalformedArtifactDescriptor;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.Artifact;
import com.braintribe.model.artifact.Property;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.declared.DeclaredArtifact;
import com.braintribe.model.artifact.declared.DeclaredDependency;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.version.Version;
import com.braintribe.utils.IOTools;

/**
 * extracts all artifacts in either a group or root directory (and filters *Test artifacts) a group determined by it
 * having a directory parent which contains a pom.xml to be used with {@link EnsureRange} task
 * 
 * @author pit
 *
 */
public class RangeExpander {
	private static Logger log = Logger.getLogger(RangeExpander.class);
	private static DeclaredArtifactMarshaller marshaller = DeclaredArtifactMarshaller.INSTANCE;
	private static Set<String> skipped = asSet(".git");
	private static String dontBuildFile = ".dontbuild";
	private static List<File> dontBuildFiles = newList();

	private static class GroupInfo {
		public String groupId;
		public String major;
		public String minor;
		// public boolean testOnly;
	}

	public static class RangeExpanderResult {
		public String range;
		public String dontbuilds;
	}

	public static interface SolutionPostProcessor {
		List<AnalysisArtifact> apply(List<AnalysisArtifact> solutions, String defaultGroupId);
	}

	/**
	 * exposed function : lists all artifacts within the structure below a directory
	 * 
	 * @param currentDirectory
	 *            - the directory to scan from
	 * @param expand
	 *            - whether to generate fully qualified names (group:artifact#version) or not only simple names
	 *            (artifact)
	 * @return - a String with all found names concatenated with "+" as provided by the {@link EnsureRange} task
	 */
	public RangeExpanderResult determineRange(File currentDirectory, boolean expand, boolean brackets, Predicate<String> artifactMatcher,
			SolutionPostProcessor solutionPostProcessor) {
		List<AnalysisArtifact> artifacts = findResultArtifacts(currentDirectory, artifactMatcher, solutionPostProcessor);

		System.out.println("Found [" + artifacts.size() + "] artifacts to be processed, and found [" + dontBuildFiles.size() + "] .dontbuild files");

		RangeExpanderResult result = new RangeExpanderResult();
		result.range = rangeString(artifacts, brackets, expand);
		result.dontbuilds = dontBuildString();
		return result;
	}

	private List<AnalysisArtifact> findResultArtifacts(File currentDirectory, Predicate<String> artifactMatcher,
			SolutionPostProcessor solutionPostProcessor) {

		List<AnalysisArtifact> solutions = newList();
		dontBuildFiles.clear();

		List<GroupInfo> groupInfos = newList();
		for (ArtifactGroup group : findArtifactGroups(currentDirectory)) {
			GroupInfo groupInfo = buildInfoFor(group);

			if (groupInfo != null) {
				groupInfos.add(groupInfo);
				for (File directory : group.getMemberDirectories())
					findArtifact(directory, groupInfo) //
							.ifPresent(solutions::add);
			}
		}

		solutions = solutionPostProcessor.apply(solutions, defaultGroupId(groupInfos));

		return solutions.stream() //
				.filter(s -> artifactMatcher.test(s.getArtifactId())) //
				.collect(Collectors.toList());
	}

	private static String defaultGroupId(List<GroupInfo> groupInfos) {
		return groupInfos.size() == 1 ? first(groupInfos).groupId : null;
	}

	private static List<ArtifactGroup> findArtifactGroups(File currentDirectory) {
		// find the directory depth where a directory Parent exists with a pom.xml the describes a Parent artifact
		File[] subs = currentDirectory.listFiles(file -> file.isDirectory() && !skipped.contains(file.getName()));

		for (File sub : subs) {
			if (sub.getName().equalsIgnoreCase("parent")) {
				// that's a group
				File dontBuild = new File(sub.getParentFile(), dontBuildFile);
				if (dontBuild.exists())
					dontBuildFiles.add(dontBuild);

				File pom = findPom(sub);
				if (pom != null)
					return asList(newArtifactGroup(pom, currentDirectory, subs));
			}
		}

		// not found, check all other directories
		List<ArtifactGroup> result = newList();
		for (File sub : subs)
			result.addAll(findArtifactGroups(sub));

		return result;
	}

	private static ArtifactGroup newArtifactGroup(File parentPom, File groupDir, File[] memberDirs) {
		ArtifactGroup group = new ArtifactGroup();
		group.setGroupDirectory(groupDir);
		group.setParentPom(parentPom);
		group.setMemberDirectories(memberDirs);
		return group;
	}

	private GroupInfo buildInfoFor(ArtifactGroup group) {
		AnalysisArtifact parent = unmarshallPom(group.getParentPom()).get();
		Version version = Version.parse(parent.getVersion());

		String major = String.valueOf(version.getMajor());
		String minor = String.valueOf(version.getMinor());

		if (missingProperty("major", major, group) || missingProperty("minor", minor, group))
			return null;

		GroupInfo context = new GroupInfo();
		context.groupId = parent.getGroupId();
		context.major = major;
		context.minor = minor;

		return context;
	}

	private static boolean missingProperty(String propertyName, String propertyValue, ArtifactGroup group) {
		if (propertyValue != null)
			return false;

		log.warn("no property [" + propertyName + "] found in [" + group.getParentPom().getAbsolutePath() + "], ignoring");
		return true;
	}

	private Optional<AnalysisArtifact> findArtifact(File directory, GroupInfo groupInfo) {
		File pom = findPom(directory);
		if (pom == null || pom.exists() == false) {
			log.warn("there is no pom in group [" + directory.getAbsolutePath() + "], artifact is skipped");
			return Optional.empty();
		}
		return Optional.of(unmarshallPom(pom).get());
	}

	private static File findPom(File dir) {
		File[] contents = dir.listFiles(file -> file.getName().equalsIgnoreCase("pom.xml"));
		return contents != null && contents.length != 0 ? contents[0] : null;
	}

	private Maybe<AnalysisArtifact> unmarshallPom(File pom) {
		final DeclaredArtifact declaredArtifact; 
		try (InputStream in = new BufferedInputStream(new FileInputStream(pom))) {
			Maybe<Object> artifactMaybe = DeclaredArtifactMarshaller.INSTANCE.unmarshallReasoned(in);
			
			if (artifactMaybe.isUnsatisfied())
				return Reasons.build(DeclaredArtifactReadError.T) //
						.text("Could not read declared artifact from: " + pom.getAbsolutePath()) //
						.cause(artifactMaybe.whyUnsatisfied())
						.toMaybe();
			declaredArtifact = (DeclaredArtifact)artifactMaybe.get();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Maybe<CompiledArtifactIdentification> identificationMaybe = DeclaredArtifactIdentificationExtractor.extractIdentification(declaredArtifact);
		
		if (identificationMaybe.isUnsatisfied()) {
			return Reasons.build(MalformedArtifactDescriptor.T) //
					.text("Could identify declared artifact from: " + pom.getAbsolutePath()) //
					.cause(identificationMaybe.whyUnsatisfied())
					.toMaybe();
		}
		
		CompiledArtifactIdentification cai = identificationMaybe.get();
		
		AnalysisArtifact artifact = acquireArtifact(cai);
		String groupId = cai.getGroupId();
		
		artifact.setArtifactId(cai.getArtifactId());
		artifact.setGroupId(groupId);
		artifact.setVersion(cai.getVersion().asString());
		
		for (DeclaredDependency dependency: declaredArtifact.getDependencies()) {
			if (!groupId.equals(dependency.getGroupId()))
				continue;
			
			AnalysisDependency analysisDependency = AnalysisDependency.T.create();
			analysisDependency.setDepender(artifact);
			analysisDependency.setSolution(acquireArtifact(analysisDependency));
			analysisDependency.setGroupId(dependency.getGroupId());
			analysisDependency.setArtifactId(dependency.getArtifactId());
			
			artifact.getDependencies().add(analysisDependency);
		}
		
		return Maybe.complete(artifact);
	}

	private Map<EqProxy<ArtifactIdentification>, AnalysisArtifact> artifactMap = new HashMap<>();
	
	private AnalysisArtifact acquireArtifact(ArtifactIdentification ai) {
		return artifactMap.computeIfAbsent(HashComparators.artifactIdentification.eqProxy(ai), k -> {
			AnalysisArtifact a = AnalysisArtifact.T.create();
			a.setGroupId(ai.getGroupId());
			a.setArtifactId(ai.getArtifactId());
			return a;
		});
	}

	@SuppressWarnings("unused") // not sure why what's the point
	private static Version versionWithRevision(String major, String minor, Artifact artifact) {
		Property revision = artifact.getProperties().stream().filter(p -> p.getName().equalsIgnoreCase("revision")).findFirst() //
				.orElseThrow(() -> new IllegalStateException("no property [revision] found"));
		return Version.parse(major + "." + minor + "." + revision.getRawValue());
	}

	private static String rangeString(List<AnalysisArtifact> artifacts, boolean brackets, boolean expand) {
		if (expand)
			return artifacts.stream() //
					.map(AnalysisArtifact::asString) //
					.map(s -> brackets ? "[" + s + "]" : s) //
					.collect(Collectors.joining("+"));
		else
			return artifacts.stream() //
					.map(AnalysisArtifact::getArtifactId) //
					.map(s -> brackets ? "[" + s + "]" : s) //
					.collect(Collectors.joining("+"));
	}

	private static String dontBuildString() {
		return dontBuildFiles.stream() //
				.map(RangeExpander::safeSlurp) //
				.map(fileContent -> fileContent.split("\n")) //
				.flatMap(Stream::of) //
				.map(String::trim) //
				.filter(s -> s.length() > 0).collect(Collectors.joining("+"));
	}

	private static String safeSlurp(File file) {
		try (InputStream stream = new FileInputStream(file)) {
			return IOTools.slurp(stream, "UTF-8");

		} catch (IOException e) {
			log.error("cannot read file [" + file.getAbsolutePath() + "]", e);
			return "";
		}
	}

	public static void main(String[] args) {
		for (String arg : args) {
			String[] params = arg.split("\\|");
			File current = new File(params[0]);
			boolean expand = Boolean.parseBoolean(params[1]);
			RegexCheck regex = new RegexCheck(params[2]);
			RangeExpanderResult result = new RangeExpander().determineRange(current, expand, false, regex, null);

			System.out.println("range : " + result.range);
			System.out.println("ignored : " + result.dontbuilds);

			/* List<ArtifactGroup> artifactDirectories = findArtifactGroups(current); System.out.println( arg); for
			 * (ArtifactGroup group : artifactDirectories) { System.out.println( "\t" +
			 * group.getGroupDirectory().getAbsolutePath() + " : " + group.getParentPom().getAbsolutePath()); } */
		}
	}
}
