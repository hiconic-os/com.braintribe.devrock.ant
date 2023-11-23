// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.utils;

import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.braintribe.build.ant.types.RangeExpander.SolutionPostProcessor;
import com.braintribe.devrock.mc.core.declared.commons.HashComparators;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.essential.ArtifactIdentification;

/**
 * This is only intended to be used once. No thread-safety BS or anything.
 * 
 * @author peter.gazdik
 */
public class DependerResolver implements SolutionPostProcessor {

	private final Set<String> rootArtifactNames;

	public DependerResolver(String rootArtifacts) {
		this.rootArtifactNames = asSet(rootArtifacts.split("\\+"));
	}

	/** Returns a sub-set of given solutions containing only rootArtifacts and their "dependers". */
	public List<AnalysisArtifact> apply(List<AnalysisArtifact> solutions, String defaultGroupId) {
		Map<String, AnalysisArtifact> index = new HashMap<>();
		
		for (AnalysisArtifact aa : solutions) {
			ArtifactIdentification ai = ArtifactIdentification.from( aa);
			index.put( ai.asString(), aa);
		}
		
		for (AnalysisArtifact solution : solutions) {
			for (AnalysisDependency dep : solution.getDependencies()) {
				String ai = dep.getGroupId() +":"+dep.getArtifactId();
				AnalysisArtifact dependerArtifact = index.get(ai);
				if (dependerArtifact != null) {
					dependerArtifact.getDependers().add( dep);
				}
			}
		}
		
		Set<AnalysisArtifact> dependers = collectRootSolutionsWithDependers(index, defaultGroupId);

		return newList(dependers);
	}

	private Set<AnalysisArtifact> collectRootSolutionsWithDependers(Map<String, AnalysisArtifact> index, String defaultGroupId) {
		Set<AnalysisArtifact> artifacts = HashComparators.analysisArtifact.newHashSet();
		
		for (String artifactName: rootArtifactNames) {
			artifactName = ensureQualified(artifactName, defaultGroupId);
			AnalysisArtifact artifact = index.get(artifactName);
			
			if (artifact != null) {
				artifacts.add(artifact);
				collectDependers(artifact, artifacts);			
			}
		}
		
		return artifacts;
	}
	
	private void collectDependers( AnalysisArtifact artifact, Set<AnalysisArtifact> artifacts) {
		for (AnalysisDependency dependerDep: artifact.getDependers()) {
			AnalysisArtifact dependerArt = dependerDep.getDepender();
			artifacts.add(dependerArt);
			collectDependers(dependerArt, artifacts);
		}	
	}
	

	private String ensureQualified(String artifactId, String defaultGroupId) {
		if (isQualified(artifactId))
			return artifactId;

		Objects.requireNonNull(defaultGroupId, () -> "Please provide groupId for artifact: " + artifactId);

		return artifactName(defaultGroupId, artifactId);
	}

	private boolean isQualified(String artifactId) {
		return artifactId.contains(":");
	}

	private String artifactName(String groupId, String artifactId) {
		return groupId + ":" + artifactId;
	}

}
