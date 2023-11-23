// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.util.Arrays;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;

public class ExtractArtifactProperties extends Task {
	private String artifact = null;
	private String groupIdProperty = null;
	private String artifactIdProperty = null;
	private String versionProperty = null;
	private String relativePathProperty = "relativePath";
	
	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}
	
	public void setGroupIdProperty(String groupIdProperty) {
		this.groupIdProperty = groupIdProperty;
	}
	
	public void setArtifactIdProperty(String artifactIdProperty) {
		this.artifactIdProperty = artifactIdProperty;
	}
	
	public void setVersionProperty(String versionProperty) {
		this.versionProperty = versionProperty;
	}
	
	public void setRelativePathProperty(String relativePathProperty) {
		this.relativePathProperty = relativePathProperty;
	}

	@Override
	public void execute() throws BuildException {
		boolean multi = artifact.contains("+");
		
		if (multi)
			extractMulti();
		else
			extractSingle();
	}

	private void extractSingle() {
		for (String value : Arrays.asList(artifact, groupIdProperty, artifactIdProperty, versionProperty)) {
			if (value == null)
				throw new BuildException(
						"the following attributes are required: [artifact, groupIdProperty, artifactIdProperty, versionProperty]"
								+ " and the following attributes are optional: [relativePathProperty]");
		}
		String groupId = null;
		String version = null;
		String artifactId = null;
		
		// support the # mark -> use Malaclypse's NameParser feature 
		if (!artifact.contains( "#")) {

			int splitIndex = artifact.indexOf(":");
			
			if (splitIndex == -1)
				throw new BuildException("artifact must match this pattern some.group.id:artifactId[-version]");
			
			groupId = artifact.substring(0, splitIndex);
			artifactId = artifact.substring(splitIndex + 1);
			
			
			splitIndex = artifactId.indexOf("-");
			
			if (splitIndex != -1) {
				version = artifactId.substring(splitIndex + 1);
				artifactId = artifactId.substring(0, splitIndex);
			}
		} else { // otherwise, use the standard.
			VersionedArtifactIdentification artifactEntity = VersionedArtifactIdentification.parse(artifact);
			groupId = artifactEntity.getGroupId();
			artifactId = artifactEntity.getArtifactId();
			version = artifactEntity.getVersion();
		}
		
		String relativePath = groupId.replace('.', '/') + "/" + artifactId + (version == "" ? "" : "/" + version);
		
		getProject().setProperty(groupIdProperty, groupId);
		getProject().setProperty(artifactIdProperty, artifactId);
		getProject().setProperty(versionProperty, version);
		getProject().setProperty(relativePathProperty, relativePath);
	}
	
	
	private void extractMulti() {
		for (String value : Arrays.asList(artifact, artifactIdProperty)) {
			if (value == null)
				throw new BuildException(
						"the following attributes are required: [artifact, artifactIdProperty]");
		}
		getProject().setProperty(artifactIdProperty, artifact);
		getProject().setProperty(groupIdProperty, "");
		getProject().setProperty(versionProperty, "");
	}
}
