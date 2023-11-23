// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.common.RegexCheck;
import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.StringTools;

/**
 * Transforms an artifacts string (as e.g. provided by {@link ExtractArtifactsFromClasspathString}). Example: <br/>
 * Input:
 * <code>com.braintribe.test.javadoc:JavadocArtifact#1.0,com.braintribe.test.javadoc:JavadocArtifactDependency#1.0</code>
 * <br/>
 * Transformation Pattern (<code>groupIdPartsSeparatorInTarget</code> set to '/'):
 * <code>path/to/repository/$GROUPID/$ARTIFACTID/$VERSION/$ARTIFACTID-$VERSION.jar</code><br/>
 * Output:
 * <code>path/to/repository/com/braintribe/test/javadoc/JavadocArtifact/1.0/JavadocArtifact-1.0.jar,path/to/repository/com/braintribe/test/javadoc/JavadocArtifactDependency/1.0/JavadocArtifactDependency-1.0.jar</code>
 *
 * @author michael.lafite
 */
public class TransformArtifactsString extends Task {

	private String artifactsString;
	private String resultProperty;
	private String transformationPattern;
	private String artifactsSeparator = ",";
	private String artifactsSeparatorInTarget; // set to [artifactsSeparator], if not set
	private String groupIdPartsSeparator = ".";
	private String groupIdPartsSeparatorInTarget; // set to [groupIdPartsSeparator], if not set
	private String groupIdAndArtifactIdSeparator = ":";
	private String artifactIdAndVersionSeparator = "#";
	private String variablePrefix = "$";
	private String variableSuffix = "";
	private String groupIdVariable = "GROUPID";
	private String artifactIdVariable = "ARTIFACTID";
	private String versionVariable = "VERSION";
	private String jarOrWarVariable = "JAR_OR_WAR";
	private String artifactIncludeRegex = ".*";
	private String artifactExcludeRegex = "";

	@Override
	public void execute() throws BuildException {
		final String transformedString = transformArtifactsString();
		getProject().setProperty(this.resultProperty, transformedString);
	}

	String transformArtifactsString() throws BuildException {

		if (this.artifactsString == null) {
			throw new BuildException("Property 'artifactsString' not set!");
		}

		if (this.resultProperty == null) {
			throw new BuildException("Property 'resultProperty' not set!");
		}

		if (this.transformationPattern == null) {
			this.transformationPattern = "$GROUPID" + this.groupIdAndArtifactIdSeparator + "$ARTIFACTID" + this.artifactIdAndVersionSeparator
					+ "$VERSION";
		}

		if (this.artifactsSeparatorInTarget == null) {
			this.artifactsSeparatorInTarget = this.artifactsSeparator;
		}

		if (this.groupIdPartsSeparatorInTarget == null) {
			this.groupIdPartsSeparatorInTarget = this.groupIdPartsSeparator;
		}

		final List<String> artifacts = CollectionTools.decodeCollection(this.artifactsString, this.artifactsSeparator, false, true, false, false);

		final List<String> transformedArtifacts = new ArrayList<String>();

		final RegexCheck regexCheck = new RegexCheck(this.artifactIncludeRegex, this.artifactExcludeRegex);

		for (final String artifact : artifacts) {

			if (!regexCheck.test(artifact)) {
				continue;
			}

			final String version = StringTools.getSubstringAfterLast(artifact, this.artifactIdAndVersionSeparator);
			final String artifactWithoutVersion = StringTools.removeLastNCharacters(artifact, version.length() + 1);
			final String artifactId = StringTools.getSubstringAfterLast(artifactWithoutVersion, this.groupIdAndArtifactIdSeparator);
			final String groupIdInSource = StringTools.removeLastNCharacters(artifactWithoutVersion, artifactId.length() + 1);
			final String groupIdInTarget = groupIdInSource.replace(this.groupIdPartsSeparator, this.groupIdPartsSeparatorInTarget);

			String transformedArtifact = replaceVariable(this.transformationPattern, this.groupIdVariable, groupIdInTarget);
			transformedArtifact = replaceVariable(transformedArtifact, this.artifactIdVariable, artifactId);
			transformedArtifact = replaceVariable(transformedArtifact, this.versionVariable, version);

			if (transformedArtifact.contains(toVariableInString(this.jarOrWarVariable))) {
				String transformedArtifactJar = replaceVariable(transformedArtifact, this.jarOrWarVariable, "jar");
				String transformedArtifactWar = replaceVariable(transformedArtifact, this.jarOrWarVariable, "war");

				File transformedArtifactJarFile = new File(transformedArtifactJar);
				File transformedArtifactWarFile = new File(transformedArtifactWar);

				if (transformedArtifactJarFile.exists() && transformedArtifactWarFile.exists()) {
					throw new BuildException("Can't transform artifact '" + artifact + "' using trasformation pattern '" + this.transformationPattern
							+ "' because jar AND war path, exist: " + transformedArtifactJar + ", " + transformedArtifactWar
							+ ". This is not expected!");
				}

				if (!transformedArtifactJarFile.exists() && !transformedArtifactWarFile.exists()) {
					throw new BuildException("Can't transform artifact '" + artifact + "' using trasformation pattern '" + this.transformationPattern
							+ "' because jar and war path both do NOT exist: " + transformedArtifactJar + ", " + transformedArtifactWar
							+ ". This is not expected!");
				}

				transformedArtifact = transformedArtifactJarFile.exists() ? transformedArtifactJar : transformedArtifactWar;

			}

			transformedArtifacts.add(transformedArtifact);
		}

		final String transformedArtifactsString = StringTools.createStringFromCollection(transformedArtifacts, this.artifactsSeparatorInTarget);
		return transformedArtifactsString;
	}

	private String replaceVariable(final String targetString, final String variableName, final String variableValue) {
		return targetString.replace(toVariableInString(variableName), variableValue);
	}

	private String toVariableInString(String variableName) {
		return this.variablePrefix + variableName + this.variableSuffix;
	}

	public void setArtifactsString(final String artifactsString) {
		this.artifactsString = artifactsString;
	}

	public void setResultProperty(final String resultProperty) {
		this.resultProperty = resultProperty;
	}

	public void setTransformationPattern(final String transformationPattern) {
		this.transformationPattern = transformationPattern;
	}

	public void setArtifactsSeparator(final String artifactsSeparator) {
		this.artifactsSeparator = artifactsSeparator;
	}

	public void setArtifactsSeparatorInTarget(final String artifactsSeparatorInTarget) {
		this.artifactsSeparatorInTarget = artifactsSeparatorInTarget;
	}

	public void setGroupIdPartsSeparator(final String groupIdPartsSeparator) {
		this.groupIdPartsSeparator = groupIdPartsSeparator;
	}

	public void setGroupIdPartsSeparatorInTarget(final String groupIdPartsSeparatorInTarget) {
		this.groupIdPartsSeparatorInTarget = groupIdPartsSeparatorInTarget;
	}

	public void setGroupIdAndArtifactIdSeparator(final String groupIdAndArtifactIdSeparator) {
		this.groupIdAndArtifactIdSeparator = groupIdAndArtifactIdSeparator;
	}

	public void setArtifactIdAndVersionSeparator(final String artifactIdAndVersionSeparator) {
		this.artifactIdAndVersionSeparator = artifactIdAndVersionSeparator;
	}

	public void setVariablePrefix(final String variablePrefix) {
		this.variablePrefix = variablePrefix;
	}

	public void setVariableSuffix(final String variableSuffix) {
		this.variableSuffix = variableSuffix;
	}

	public void setGroupIdVariable(final String groupIdVariable) {
		this.groupIdVariable = groupIdVariable;
	}

	public void setArtifactIdVariable(final String artifactIdVariable) {
		this.artifactIdVariable = artifactIdVariable;
	}

	public void setVersionVariable(final String versionVariable) {
		this.versionVariable = versionVariable;
	}

	public void setArtifactIncludeRegex(final String artifactIncludeRegex) {
		this.artifactIncludeRegex = artifactIncludeRegex;
	}

	public void setArtifactExcludeRegex(final String artifactExcludeRegex) {
		this.artifactExcludeRegex = artifactExcludeRegex;
	}

}
