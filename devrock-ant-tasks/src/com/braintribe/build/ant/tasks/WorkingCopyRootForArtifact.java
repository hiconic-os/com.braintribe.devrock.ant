// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class WorkingCopyRootForArtifact extends Task {

	private String groupId;
	private String artifactId;
	private String version;
	private File artifactVersionFolder;
	
	private String targetProperty;
	
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public void setArtifactVersionFolder(File artifactVersionFolder) {
		this.artifactVersionFolder = artifactVersionFolder;
	}
	
	public void setTargetProperty(String targetProperty) {
		this.targetProperty = targetProperty;
	}

	@Override
	public void execute() throws BuildException {
		String relativeArtifactPath = groupId.replace('.', File.separatorChar) + File.separatorChar + artifactId + File.separatorChar + version;
		String absoluteArtifactPath = artifactVersionFolder.getAbsolutePath();
		String artifactRoot = absoluteArtifactPath.substring(0,absoluteArtifactPath.length()-relativeArtifactPath.length());
		
		getProject().setProperty(targetProperty, artifactRoot);
		
		
	}
	
}
