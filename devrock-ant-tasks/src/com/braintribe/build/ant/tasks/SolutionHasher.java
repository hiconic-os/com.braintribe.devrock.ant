// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newMap;
import static com.braintribe.utils.lcd.StringTools.isEmpty;
import static java.util.Collections.emptyMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.ant.tasks.malaclypse.ArtifactExclusionList;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.utils.CommonTools;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;

/**
 * to debug: set/export ANT_OPTS='-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y'
 *
 * @author pit
 */
public class SolutionHasher extends Task {

	public static final String MISSING_SOLUTIONS_FILE_NAME = "missing.txt";

	private String range;
	private String ignoreRange;
	private String knownHashes;
	private String useScope;
	private String tagRule;
	private String typeFilter = "*";
	private File targetDirectory;
	private String hashAlgo = "MD5";
	private String suffix = "hash.txt";
	private File missingSolutionsFile;
	private boolean details = true;

	// mutable variables
	private Map<String, List<CompiledDependencyIdentification>> groups;
	private Map<String, String> artifactToHash = emptyMap();

	private String exclusionDependency;

	@Configurable
	public void setTagRule(String tagRule) {
		this.tagRule = tagRule;
	}
	
	@Configurable
	public void setTypeFilter(String typeFilter) {
		this.typeFilter = typeFilter;
	}
	
	@Required
	public void setRange(String range) {
		this.range = range;
	}
	
	@Configurable
	public void setIgnoreRange(String ignoreRange) {
		this.ignoreRange = ignoreRange;
	}

	@Required
	public void setTargetDirectory(File targetDirectory) {
		this.targetDirectory = targetDirectory;
	}

	@Configurable
	public void setMissingSolutionsFile(File missingSolutionsFile) {
		this.missingSolutionsFile = missingSolutionsFile;
	}
	
	@Configurable
	public void setExclusionDependency(String exclusionDependency) {
		this.exclusionDependency = exclusionDependency;
	}

	/**
	 * For optimization purposes we can provide hashes for individual artifacts who'se hashes we already know, thus avoiding the need to resolve it's
	 * dependencies. The value is expected in the format: <code>${groupId1}->${artifactId1}=${hash1},${artifactId2}=${hash2};${groupId2}->...</code>
	 */
	@Configurable
	public void setKnownHashes(String knownHashes) {
		this.knownHashes = knownHashes;
	}

	@Configurable
	public void setUseScope(String scope) {
		useScope = scope;
	}

	@Configurable
	public void setHashAlgorithm(String hashAlgo) {
		this.hashAlgo = hashAlgo;
	}

	@Configurable
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	@Configurable
	public void setDetails(boolean details) {
		this.details = details;
	}

	@Override
	public void execute() throws BuildException {
		new ColorSupport(getProject()).installConsole();
		setDefaultMissingSolutionsFileIfNeeded();
		parseRange();
		parseKnownHashes();
		computeAllTheHashes();
	}

	private void setDefaultMissingSolutionsFileIfNeeded() {
		if (missingSolutionsFile != null)
			return;

		missingSolutionsFile = new File(targetDirectory, MISSING_SOLUTIONS_FILE_NAME);
		if (missingSolutionsFile.exists())
			missingSolutionsFile.delete();

	}

	/**
	 * lill' filter to filter-out the {@link ArtifactIdentification} that came in via the 'ignore-range' property
	 * @author pit
	 *
	 */
	private class ExclusionFilter implements Predicate<CompiledDependencyIdentification> {
		
		private Set<ArtifactIdentification> ais;

		public ExclusionFilter(Set<ArtifactIdentification> ais) {
			this.ais = ais;						
		}

		@Override
		public boolean test(CompiledDependencyIdentification t) {
			for (ArtifactIdentification ai : ais) {
				if (ai.compareTo(t) == 0)
					return false;
			}
			return true;
		}		
	}
	
	private void parseRange() {
		// build a filter to get rid of the ignore-range artifacts
		Predicate<CompiledDependencyIdentification> filter = (c) -> true;
		if (ignoreRange != null && ignoreRange.length() > 0) {
			Set<ArtifactIdentification> exclusionsPerRange = new ArtifactExclusionList( ignoreRange.trim()).getExclusions();
			filter = new ExclusionFilter( exclusionsPerRange);
		}
		
		groups = Stream.of(range.split("\\+")) //
				.map(CompiledDependencyIdentification::parseAndRangify) //
				.filter( filter) // filter ignore range
				.collect(Collectors.groupingBy(CompiledDependencyIdentification::getGroupId));
	}

	private void parseKnownHashes() {
		if (isEmpty(knownHashes))
			return;

		artifactToHash = newMap();

		for (String knownHashGroupEntry : knownHashes.split(";")) {
			// knownHashGroupEntry format: groupId->artifactId1:hash1,artifactId2:hash2,...
			String[] groupAndArtifactEntries = knownHashGroupEntry.split("::");
			if (groupAndArtifactEntries.length != 2)
				throw new BuildException("Unable to parse known hashes. One of the entries seems wrong: " + knownHashGroupEntry);

			String groupId = groupAndArtifactEntries[0];
			String artifactHashes = groupAndArtifactEntries[1]; // artifactId1:hash1,artifactId2:hash2,...

			for (String artifactEntry : artifactHashes.split(",")) {
				String[] artifactAndHash = artifactEntry.split("=");
				if (artifactAndHash.length != 2)
					throw new BuildException("Unable to parse known hashes. One of the entries seems wrong. Group entry: " + knownHashGroupEntry
							+ ", problematic artifact entry: " + artifactEntry);

				artifactToHash.put(groupId + ":" + artifactAndHash[0], artifactAndHash[1]);
			}
		}
	}

	private void computeAllTheHashes() {
		String allGroupsHash = hashGroups();

		FileTools.write(new File(targetDirectory, suffix)).string(allGroupsHash);
	}

	private String hashGroups() {
		File output = new File(targetDirectory, "groups." + suffix);

		try (FileOutputStream stream = new FileOutputStream(output);) {
			DigestOutputStream digestStream = new DigestOutputStream(stream, getMessageDigest());

			String lineBeginning = "";
			for (Entry<String, List<CompiledDependencyIdentification>> entry : groups.entrySet()) {
				String groupId = entry.getKey();
				String groupHash = hashGroup(groupId, entry.getValue());
				String line = lineBeginning + groupId + " " + groupHash;
				digestStream.write(line.getBytes());
				lineBeginning = "\n";
			}

			return toLowerCaseHex(digestStream);

		} catch (IOException e) {
			throw new BuildException("cannot process groups", e);
		}
	}

	/**
	 * process a group, i.e process every single artifact in it, and write a file with the name of artifact and the hashcode of its dependencies
	 * 
	 * @return - the hash for the dependencies
	 */
	private String hashGroup(String groupId, List<CompiledDependencyIdentification> dependencies) {
		// where to store the files?
		File output = new File(targetDirectory, groupId + "." + suffix);

		try (FileOutputStream stream = new FileOutputStream(output);) {
			DigestOutputStream digestStream = new DigestOutputStream(stream, getMessageDigest());

			String lineBeginning = "";
			for (CompiledDependencyIdentification dependency : dependencies) {				
				Maybe<CompiledArtifactIdentification> solutionMaybe = Bridges.getInstance(getProject()).resolveDependencyAsMaybe(dependency);
				if (solutionMaybe.isSatisfied()) {
					CompiledArtifactIdentification solution = solutionMaybe.get();
					String solutionHash = hashSolution(solution);
					String line = lineBeginning + solution.asString() + " " + solutionHash;
					digestStream.write(line.getBytes());
					lineBeginning = "\n";
				}
				else {
					// just write out the name of the dependency that can't be resolved
					IOTools.spit(missingSolutionsFile, dependency.asString(), "UTF-8", true);
				}
			}

			return toLowerCaseHex(digestStream);

		} catch (IOException e) {
			throw new BuildException("cannot process group [" + groupId + "]", e);
		}
	}

	/**
	 * process a single dependency, i.e. make a walk and create a hash over all its dependencies
	 * 
	 * @return - a hash code over all dependencies
	 */
	private String hashSolution(CompiledArtifactIdentification solution) {
		String solutionHash = resolveKnownSolutionHash(solution);
		if (solutionHash != null)
			return solutionHash;
		
		McBridge mcBridge = Bridges.getInstance(getProject());
		
		// exclusion list 
		Set<ArtifactIdentification> exclusions = null;
		if (exclusionDependency != null) {		
			
			PartIdentification exclusionsPart = PartIdentification.create("exclusions");
			
			com.braintribe.model.artifact.consumable.Artifact exclusionArtifact = mcBridge.resolveArtifact(CompiledDependencyIdentification.parse(exclusionDependency), exclusionsPart);
			
			com.braintribe.model.artifact.consumable.Part part = exclusionArtifact.getParts().get(exclusionsPart.asString());
			
			if (part == null)
				throw new IllegalStateException("Missing exclusions part in artifact: " + exclusionArtifact.asString());
			
			
			try (InputStream in = part.getResource().openStream()) {
				exclusions = new ArtifactExclusionList( IOTools.slurp(in, "UTF-8")).getExclusions();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	
		// run a dependency walk
		/*
		List<AnalysisArtifact> rawSolutions = mcBridge.resolveClasspath(
				Collections.singletonList(CompiledTerminal.from(solution)), 
				useScope, 
				tagRule, 
				typeFilter, 
				Collections.emptyList(), 
				exclusions).getSolutions();
		*/
		
		CompiledArtifact resolvedArtifact;
		try {
			resolvedArtifact = mcBridge.resolveArtifact(solution);
		} catch (Exception e) {
			if (e instanceof ReasonException) {
				ReasonException r = (ReasonException) e;
				throw mcBridge.produceContextualizedBuildException("cannot resolve artifact for [" + solution.asString() +"] as " + r.getReason().stringify(), null);				
			}
			else {
				throw mcBridge.produceContextualizedBuildException( "cannot resolve artifact for [" + solution.asString() +"] as " + e.getMessage(), null);
			}
		}
		AnalysisArtifactResolution resolution = mcBridge.resolveClashfreeRelevantSolutions( Collections.singletonList( CompiledTerminal.from( resolvedArtifact)), useScope, tagRule, typeFilter, exclusions);
		
		// if details are requested, write out the file with the resolution 
		if (details) {
			String caiAsString = solution.asString();
			caiAsString = caiAsString.replace(':', '.');
			File dumpFile = new File( targetDirectory, caiAsString + ".resolution.yaml");		
			mcBridge.writeResolutionToFile(resolution, dumpFile);
		}
		
		if (resolution.hasFailed()) {
			VersionedArtifactIdentification vai = VersionedArtifactIdentification.parse( solution.asString());
			String vaiAsString = vai.asString();
			vaiAsString = vaiAsString.replace(':', '.');
			File dumpFile = new File( targetDirectory, vaiAsString + ".resolution.yaml");		
			mcBridge.writeResolutionToFile(resolution, dumpFile);
			throw mcBridge.produceContextualizedBuildException( "cannot build clash free dependency tree for [" + solution.asString() +"] as " + resolution.getFailure().stringify(), null);
		}
		
		List<AnalysisArtifact> rawSolutions = resolution.getSolutions();
		
		
		System.out.println( "solutions of [" + solution.asString() + "]: " + rawSolutions.stream().map( s -> s.asString()).collect(Collectors.joining(",")));
		
		MessageDigest digest = getMessageDigest();

		rawSolutions.stream() //
				.map(AnalysisArtifact::asString) //
				.sorted() //
				.map(String::getBytes) //
				.forEach(digest::update);

		return toLowerCaseHex(digest);
	}

	private String resolveKnownSolutionHash(CompiledArtifactIdentification solution) {
		String versionlessName = solution.getGroupId() + ":" + solution.getArtifactId();
		return artifactToHash.get(versionlessName);
	}

	private MessageDigest getMessageDigest() {
		try {
			return MessageDigest.getInstance(hashAlgo);
		} catch (NoSuchAlgorithmException e) {
			throw new BuildException("cannot create message digest", e);
		}
	}

	private String toLowerCaseHex(DigestOutputStream digestStream) throws IOException {
		digestStream.flush();
		return toLowerCaseHex(digestStream.getMessageDigest());
	}

	private String toLowerCaseHex(MessageDigest md) {
		return CommonTools.asString(md.digest());
	}


}
