// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.utils.StringTools;

/**
 * Extracts one or more solutions from a {@link DependenciesTask#setSolutionListStringProperty(String)
 * solution list string}. Example:<br>
 * Input:<br>
 * <code>solutionListString: com.braintribe.common:codec-api:1.0.9,com.braintribe.common:logging-ndc:1.0.8,com.braintribe.common:logging:1.0.8</code><br>
 * <code>artifacts: com.braintribe.common:codec-api,com.braintribe.common:logging-ndc</code><br>
 * Output:<br>
 * <code>com.braintribe.common:codec-api:1.0.9,com.braintribe.common:logging-ndc:1.0.8</code>
 *
 * @author michael.lafite
 */
public class ExtractSolutionsFromSolutionListString extends Task {

	private String solutionListString;
	private String artifacts;
	private String resultProperty;

	public void setSolutionListString(String solutionListString) {
		this.solutionListString = solutionListString;
	}

	public void setArtifacts(String artifact) {
		this.artifacts = artifact;
	}

	public void setResultProperty(String resultProperty) {
		this.resultProperty = resultProperty;
	}

	@Override
	public void execute() throws BuildException {
		if (solutionListString == null) {
			throw new BuildException("Property 'solutionListString' not set!");
		}

		if (artifacts == null) {
			throw new BuildException("Property 'artifacts' not set!");
		}

		if (resultProperty == null) {
			throw new BuildException("Property 'resultProperty' not set!");
		}

		final String result = extractSolutionFromSolutionListString(solutionListString, artifacts);
		getProject().setProperty(this.resultProperty, result);
	}

	static String extractSolutionFromSolutionListString(String solutionListString, String artifactListString) throws BuildException {
		List<String> artifacts = Arrays.asList(StringTools.splitCommaSeparatedString(artifactListString, false));
		List<String> allSolutions = Arrays.asList(StringTools.splitCommaSeparatedString(solutionListString, false));
		List<String> searchedSolutions = new ArrayList<>();

		for (String artifact : artifacts) {
			String searchedSolution = null;
			for (String solution : allSolutions) {
				if (solution.startsWith(artifact)) {
					searchedSolution = solution;
					break;
				}
			}
			if (searchedSolution == null) {
				throw new BuildException("Couldn't find solution for artifact '" + artifact + "' in solution list '" + solutionListString + "'!");
			}
			searchedSolutions.add(searchedSolution);
		}
		String result = searchedSolutions.stream().collect(Collectors.joining(","));
		return result;
	}
}
