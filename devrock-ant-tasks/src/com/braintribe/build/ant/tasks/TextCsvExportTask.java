// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.translation.model.Model;

public class TextCsvExportTask extends Task {
	
	private String sourcePath;
	
	private String csvFilePath;
	
	private String groupId;
	
	private String artifactId;
	
	private String version;

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getCsvFilePath() {
		return csvFilePath;
	}

	public void setCsvFilePath(String csvFilePath) {
		this.csvFilePath = csvFilePath;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	@Override
	public void execute() throws BuildException {
		Model model = new Model();
		
		model.updateModelFromCsv(csvFilePath);
		model.updateModelFromSourcePath(sourcePath, groupId, artifactId, version);
		model.writeModeltoCsv(csvFilePath);
	}

}
