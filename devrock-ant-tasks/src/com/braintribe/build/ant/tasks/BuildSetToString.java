// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;

import com.braintribe.devrock.mc.api.repository.CodebaseReflection;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.utils.CommonTools;
import com.braintribe.utils.StringTools;

/**
 * Stores the build set, i.e. the list of the dependencies to be built, as a string in a configurable property.
 * 
 * <p>
 * In addition to the attributes inherited from {@link AbstractBuildSetTask}, the following attributes are supported:
 * <ul>
 * <li><code>resulProperty</code>: the name of the result property. This is mandatory.
 * <li><code>artifactSeparator</code>: the delimiter between artifacts. Default: {@value #DEFAULT_ARTIFACT_SEPARATOR}
 * <li><code>artifactGroupSeparator</code>: the delimiter between artifacts groups. Default:
 * {@value #DEFAULT_ARTIFACTGROUP_SEPARATOR}
 * <li><code>artifactGroupAndIdSeparator</code>: the delimiter between the last group and the artifact id. Default:
 * {@value #DEFAULT_ARTIFACTGROUPANDID_SEPARATOR}
 * <li><code>artifactIdAndVersionSeparator</code>: the delimiter between artifact id and version. Default:
 * {@value #DEFAULT_ARTIFACTIDANDVERSION_SEPARATOR}
 * </ul>
 * 
 * @author michael.lafite
 */
public class BuildSetToString extends ForBuildSet {

	public static final String DEFAULT_ARTIFACT_SEPARATOR = ";";
	public static final String DEFAULT_ARTIFACTGROUP_SEPARATOR = ".";
	public static final String DEFAULT_ARTIFACTGROUPANDID_SEPARATOR = ":";
	public static final String DEFAULT_ARTIFACTIDANDVERSION_SEPARATOR = "#";

	private String artifactSeparator = DEFAULT_ARTIFACT_SEPARATOR;
	private String artifactGroupSeparator = DEFAULT_ARTIFACTGROUP_SEPARATOR;
	private String artifactGroupAndIdSeparator = DEFAULT_ARTIFACTGROUPANDID_SEPARATOR;
	private String artifactIdAndVersionSeparator = DEFAULT_ARTIFACTIDANDVERSION_SEPARATOR;
	private String resultProperty;

	public void setArtifactSeparator(String artifactSeparator) {
		this.artifactSeparator = artifactSeparator;
	}

	public void setArtifactGroupSeparator(String artifactGroupSeparator) {
		this.artifactGroupSeparator = artifactGroupSeparator;
	}

	public void setArtifactGroupAndIdSeparator(String artifactGroupAndIdSeparator) {
		this.artifactGroupAndIdSeparator = artifactGroupAndIdSeparator;
	}

	public void setArtifactIdAndVersionSeparator(String artifactIdAndVersionSeparator) {
		this.artifactIdAndVersionSeparator = artifactIdAndVersionSeparator;
	}

	public void setResultProperty(String resultProperty) {
		this.resultProperty = resultProperty;
	}

	@Override
	public void init() throws BuildException {
		super.init();
		if (CommonTools.isEmpty(this.resultProperty)) {
			throw new BuildException("Attribute 'resultProperty' not specified!");
		}
	}
	
	@Override
	protected void process(CodebaseReflection codebaseReflection, AnalysisArtifactResolution resolution) {
		StringBuilder builder = new StringBuilder();
		for (AnalysisArtifact artifact : resolution.getSolutions()) {
			builder.append(artifact.getGroupId().replace(".", this.artifactGroupSeparator) + this.artifactGroupAndIdSeparator + artifact.getArtifactId() + this.artifactIdAndVersionSeparator + artifact.getVersion()
					+ this.artifactSeparator);
		}
		String result = builder.toString();
		if (!result.isEmpty()) {
			result = StringTools.removeSuffix(result, this.artifactSeparator);
		}

		getProject().setProperty(this.resultProperty, result);
	}
}
