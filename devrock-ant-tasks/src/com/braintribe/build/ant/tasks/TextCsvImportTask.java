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

import com.braintribe.build.ant.translation.model.Model;
import com.braintribe.build.ant.translation.model.SynchronizationParams;

public class TextCsvImportTask extends Task {
	
	private String sourcePath;
	
	private String csvFilePath;
	
	private String groupId;
	
	private String artifactId;
	
	private String version;
	
	private String outdir = null;
	
	private SynchronizationParams.PreferredSource preferredSource = SynchronizationParams.PreferredSource.to;

	public void setPreferredSource(
			SynchronizationParams.PreferredSource preferredSource) {
		this.preferredSource = preferredSource;
	}
	
	public void setOutdir(String outdir) {
		this.outdir = outdir;
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
	
	@Override
	public void execute() throws BuildException {
		Model csvModel = new Model();
		Model sourceModel = new Model();
		csvModel.updateModelFromCsv(csvFilePath);
		
		sourceModel.updateModelFromSourcePath(sourcePath, groupId, artifactId, version);
		
		System.out.println("to artifacts: " + sourceModel.getArtifactBundles().keySet());
		
		SynchronizationParams params = new SynchronizationParams();
		params.setPreferredSource(preferredSource);
		
		sourceModel.syncFrom(csvModel, params);
		
		File outputSourceFolder = new File(sourcePath);
		if (outdir != null)
			outputSourceFolder = new File(outputSourceFolder, outdir);
		
		sourceModel.updateSourceFromModel(outputSourceFolder.toString(), groupId, artifactId, version);
	}
}
