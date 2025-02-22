//============================================================================
//BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
//Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
//It is strictly forbidden to copy, modify, distribute or use this code without written permission
//To this file the Braintribe License Agreement applies.
//============================================================================


package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;

public class Artifact extends Task {

	private static Logger log = Logger.getLogger(Artifact.class);
	
	private Artifact sibling;
	
	private String id;
	private String refid;
	
	private String groupId;
	private String artifactId;
	private String version;
	
	private String name;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRefid() {
		return refid;
	}

	public void setRefid(String refid) {
		this.refid = refid;
	}

	public String getGroupId() {		
		transfer();				
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {	
		transfer();
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getVersion() {		
		transfer();
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getName() {
		if (name == null) {
			if (groupId == null)
				transfer();
			if (name == null) {
				name = groupId +":" +artifactId + "#" + version;
			}			
		}
	
		return name;
	}

	public void setName(String name) {
		this.name = name;
		// parse.. 
		try {
			VersionedArtifactIdentification iArtifact = VersionedArtifactIdentification.parse(name);
			groupId = iArtifact.getGroupId();
			artifactId = iArtifact.getArtifactId();
			version = iArtifact.getVersion();
		} catch (Exception e) {
			String msg = "cannot parse condensed name [" + name + "]";
			log.error( msg, e);
			throw new BuildException( msg, e);
		} 
		
	}

	@Override
	public void execute() throws BuildException {	
		/*NO OP*/
	}
	
	private Artifact getSibling() { 
		if (sibling != null)
			return sibling;
		if (refid == null)
			return null;
		Project project = getProject();
		if (project == null)
			return null;
		Object obj = project.getReference( refid);
		
		if (obj == null)
			return null;
		if (obj instanceof Artifact == false)
			return null;
		sibling = (Artifact) obj;
		return sibling;
		
	}

	private void transfer() {
		if (
				groupId == null ||
				artifactId == null ||
				version == null
			)
			getSibling();
		
		if (sibling == null)
			return;
		
		groupId = sibling.groupId;
		artifactId = sibling.artifactId;
		version = sibling.version;
		name = sibling.name;
			
	}
	
	
	
}

