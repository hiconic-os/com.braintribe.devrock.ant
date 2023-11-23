// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.types;

import java.io.File;

import org.apache.tools.ant.ProjectComponent;

import com.braintribe.model.artifact.essential.PartIdentification;

/**
 * mimics a maven attachement 
 * <br/>
 * maven style :<br/>
 * &lt;attach file="${dist}/${versionedName}-sources.jar" classifier="sources" type="jar"/&gt;<br/>
 * or<br/>
 * &lt;attach file="${dist}/${versionedName}-sources.jar" type="jar"/&gt;<br/>
 * <br/>
 * file and type seem to be required (or used within maven's install/deploy)
 * 
 * ok, here it goes:<br/>
 * if you leave the file empty, then the classifier and type are used as in MC style, so from the two values, a {@link PartIdentification} is built.<br/>
 * A list of possible candidates is built. Any matching file is then attached to the task (install or deploy)<br/>
 * see {@link BasicInstallOrDeployTask} for more information
 * @author pit
 *
 */
public class AttachedResource extends ProjectComponent {

	private File file;
	private String type;
	private String classifier;
	private boolean skipIfNoFile;

	// @formatter:off
	public File getFile() { return file; }
	public void setFile(File file) { this.file = file; }
	
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	
	public String getClassifier() { return classifier; }
	public void setClassifier(String classifier) { this.classifier = classifier; }
	
	public boolean getSkipIfNoFile() { return skipIfNoFile; }
	public void setSkipIfNoFile(boolean skipIfNoFile) { this.skipIfNoFile = skipIfNoFile; }
	// @formatter:on
}
