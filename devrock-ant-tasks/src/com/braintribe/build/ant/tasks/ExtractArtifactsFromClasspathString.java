// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.utils.CollectionTools;
import com.braintribe.utils.CommonTools;
import com.braintribe.utils.StringTools;

/**
 * Extracts artifacts from a classpath string. Example:<br/>
 * Input:
 * <code>C:/path/to/repo/com/braintribe/test/javadoc/JavadocArtifact/1.0/JavadocArtifact-1.0.jar;C:/path/to/repo/com/braintribe/test/javadoc/JavadocArtifactDependency/1.0/JavadocArtifactDependency-1.0.jar</code>
 * <br/>
 * Output:
 * <code>com.braintribe.test.javadoc:JavadocArtifact#1.0;com.braintribe.test.javadoc:JavadocArtifactDependency#1.0</code>
 * <p/>
 * The string can then by transformed using {@link TransformArtifactsString}.
 *
 * @author michael.lafite
 */
public class ExtractArtifactsFromClasspathString extends Task {

	private String classpathString;
	private String repositoryDir;
	private String resultProperty;
	private String pathsSeparator = System.getProperty("path.separator");
	private String artifactsSeparator = ";";
	private final String groupIdPartsSeparator = ".";
	private String groupIdAndArtifactIdSeparator = ":";
	private String artifactIdAndVersionSeparator = "#";

	@Override
	public void execute() throws BuildException {
		final String artifactsString = extractArtifactsFromPath();
		getProject().setProperty(this.resultProperty, artifactsString);
	}

	String extractArtifactsFromPath() throws BuildException {

		if (this.classpathString == null) {
			throw new BuildException("Property 'classpathString' not set!");
		}

		if (this.repositoryDir == null) {
			throw new BuildException("Property 'repositoryDir' not set!");
		}

		if (this.resultProperty == null) {
			throw new BuildException("Property 'resultProperty' not set!");
		}

		String prefixToRemove = CommonTools.normalizePath(this.repositoryDir);
		if (!prefixToRemove.endsWith("/")) {
			prefixToRemove += "/";
		}

		final List<String> paths = CollectionTools.decodeCollection(this.classpathString, this.pathsSeparator, false,
				true, false, false);

		final List<String> artifacts = new ArrayList<String>();

		for (final String path : paths) {

			final String normalizedPath = CommonTools.normalizePath(path);

			if (!normalizedPath.startsWith(prefixToRemove)) {
				throw new BuildException("Cannot extract artifact from path '" + path
						+ "', because it doesn't start with expected prefix '" + this.repositoryDir
						+ "'! The specified classpath string is invalid: " + this.classpathString);
			}

			final String normalizedRelativePath = StringTools.removeFirstNCharacters(normalizedPath,
					prefixToRemove.length());

			final String normalizedRelativePathWithoutFile = StringTools.getSubstringBeforeLast(normalizedRelativePath,
					"/");

			final String version = StringTools.getSubstringAfterLast(normalizedRelativePathWithoutFile, "/");
			final String normalizedRelativePathWithoutVersion = StringTools.removeLastNCharacters(
					normalizedRelativePathWithoutFile, version.length() + 1);
			final String artifactId = StringTools.getSubstringAfterLast(normalizedRelativePathWithoutVersion, "/");
			final String normalizedRelativePathWithoutArtifactId = StringTools.removeLastNCharacters(
					normalizedRelativePathWithoutVersion, artifactId.length() + 1);
			final String groupId = normalizedRelativePathWithoutArtifactId.replace("/", this.groupIdPartsSeparator);

			artifacts.add(groupId + this.groupIdAndArtifactIdSeparator + artifactId
					+ this.artifactIdAndVersionSeparator + version);
		}

		final String artifactsString = StringTools.createStringFromCollection(artifacts, this.artifactsSeparator);
		return artifactsString;
	}

	public void setClasspathString(final String classpathString) {
		this.classpathString = classpathString;
	}

	public void setRepositoryDir(final String repositoryDir) {
		this.repositoryDir = repositoryDir;
	}

	public void setResultProperty(final String resultProperty) {
		this.resultProperty = resultProperty;
	}

	public void setPathsSeparator(final String pathsSeparator) {
		this.pathsSeparator = pathsSeparator;
	}

	public void setArtifactsSeparator(final String artifactsSeparator) {
		this.artifactsSeparator = artifactsSeparator;
	}

	public String getGroupIdPartsSeparator() {
		return this.groupIdPartsSeparator;
	}

	public void setGroupIdAndArtifactIdSeparator(final String groupIdAndArtifactIdSeparator) {
		this.groupIdAndArtifactIdSeparator = groupIdAndArtifactIdSeparator;
	}

	public void setArtifactIdAndVersionSeparator(final String artifactIdAndVersionSeparator) {
		this.artifactIdAndVersionSeparator = artifactIdAndVersionSeparator;
	}

}
