package com.braintribe.build.ant.tasks.malaclypse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.braintribe.model.artifact.Dependency;
import com.braintribe.model.artifact.PartTuple;
import com.braintribe.model.artifact.Solution;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.processing.version.VersionProcessor;
import com.braintribe.model.artifact.processing.version.VersionRangeProcessor;
import com.braintribe.model.artifact.version.Version;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;

public class LegacySolutionListTransposer {
	
	private Map<AnalysisArtifact, Solution> artifactToSolution = new HashMap<>();
	private Map<AnalysisDependency, Dependency> dependenciesMappings = new HashMap<>();
	
	public List<Solution> transpose(List<AnalysisArtifact> solutions) {
		List<Solution> transposedSolutions = new ArrayList<>(solutions.size());
		for (AnalysisArtifact solution: solutions) {
			transposedSolutions.add(acquireSolution(solution));
		}
		
		return transposedSolutions;
	}
	
	private Dependency acquireDependency(AnalysisDependency dependency) {
		Dependency legacyDependency = dependenciesMappings.get(dependency);
		
		if (legacyDependency == null) {
			legacyDependency = Dependency.T.create();
			dependenciesMappings.put(dependency, legacyDependency);

			transpose(dependency, legacyDependency);
		}
			
		return legacyDependency;
	}
	
	private void transpose(AnalysisDependency dependency, Dependency legacyDependency) {
		legacyDependency.setGroupId(dependency.getGroupId());
		legacyDependency.setArtifactId(dependency.getArtifactId());
		legacyDependency.setVersionRange(VersionRangeProcessor.createFromString(dependency.getVersion()));
		legacyDependency.setScope(dependency.getScope());
		legacyDependency.setOptional(dependency.getOptional());
		
		AnalysisArtifact solution = dependency.getSolution();
		
		if (solution != null)
			legacyDependency.getSolutions().add(acquireSolution(solution));
			
		AnalysisArtifact depender = dependency.getDepender();
		
		if (depender != null)
			legacyDependency.getRequestors().add(acquireSolution(depender));
	}


	private Solution acquireSolution(AnalysisArtifact artifact) {
		
		Solution solution = artifactToSolution.get(artifact);
		
		if (solution == null) {
			solution = transpose(artifact);
			artifactToSolution.put(artifact, solution);
		}
		
		return solution;
	}
	
	private Solution transpose(AnalysisArtifact artifact) {
		Version legacyVersion = VersionProcessor.createFromString(artifact.getVersion());
		Solution solution = Solution.T.create();
		solution.setGroupId(artifact.getGroupId());
		solution.setArtifactId(artifact.getArtifactId());
		solution.setVersion(legacyVersion);
		solution.setPackaging(artifact.getOrigin().getPackaging());
		
		
		for (Part part: artifact.getParts().values()) {
			Resource resource = part.getResource();
			
			if (!(resource instanceof FileResource))
				continue;
			
			FileResource fileResource = (FileResource)resource;

			com.braintribe.model.artifact.Part transposedPart = createPart(artifact.getOrigin(), legacyVersion, fileResource.getPath(), part);
			solution.getParts().add(transposedPart);
		}
		

		for (AnalysisDependency dependency: artifact.getDependers()) {
			Dependency legacyDependency = acquireDependency(dependency);
			solution.getDependencies().add(legacyDependency);
		}
		
		for (AnalysisDependency dependency: artifact.getDependers()) {
			Dependency legacyDependency = acquireDependency(dependency);
			solution.getRequestors().add(legacyDependency);
		}
		
		return solution;
	}
	
	public static com.braintribe.model.artifact.Part createPart(CompiledArtifact artifact, Version version, String file, PartIdentification partIdentification) {
		return createPart(artifact, version, file, partIdentification.getClassifier(), partIdentification.getType());
	}
	
	public static com.braintribe.model.artifact.Part createPart(CompiledArtifact artifact, Version version, String file, String classifier, String type) {
		// TODO which other qualities are needed on the legacyPart
		com.braintribe.model.artifact.Part part = com.braintribe.model.artifact.Part.T.create();
		PartTuple partTuple = PartTuple.T.create();
		partTuple.setClassifier(classifier);
		partTuple.setType(type);
		
		part.setType(partTuple);
		part.setGroupId(artifact.getGroupId());
		part.setArtifactId(artifact.getArtifactId());
		part.setVersion(version != null? version: VersionProcessor.createFromString(artifact.getVersion().asString()));
		
		part.setLocation(file);
		
		return part;
	}
	
	
}
