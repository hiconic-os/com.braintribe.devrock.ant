// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.resource.FileResource;



/**
 * 
 * build task to determine (and download) the ant build file stored as an attribute
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 *	
 * @author Pit
 *
 */
public class ResolverTask extends Task {
		
	private String groupId;
	private String artifactId;
	private String version;
	private String type;
	private String classifier;
	
	private String property;
	
	
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public void setType(String type) {
		this.type = type;
	}
	public void setClassifier(String classifier) {
		this.classifier = classifier;
	}
	public void setProperty(String propertyName) {
		this.property = propertyName;
	}

	@Override
	public void execute() throws BuildException {
		

		//
		// check parameters... 
		//
		if (
				(groupId == null) ||
				(artifactId == null) ||
				(version == null)
			){
			String msg ="Parameters [groupId, artifactId, version] must be set";
			throw new BuildException( msg);
		}
		
		if (property == null) {
			String msg ="Parameters [property] must be set";
			throw new BuildException( msg);
		}
		
		PartIdentification partType = null;
		try {
			partType = PartIdentification.parse(type);
		} catch (Exception e) {
			String msg = "type [" + type + "] is not a valid value for PartIdentification";
			throw new BuildException(msg, e);
		}
		
		final CompiledDependencyIdentification cdi;
		try {
			cdi = CompiledDependencyIdentification.create(groupId, artifactId, version);
		} catch (Exception e) {
			throw new BuildException("Invalid artifact identification groupId [" + groupId + "], artifactId [" + artifactId + "], version [" + version, e);
		}
		
//		TODO: what is this?
//		if (classifier != null) {
//			dependency.setClassifier(classifier);
//		}
		
		final Artifact resolvedArtifact; 
		
		McBridge mcBridge = Bridges.getInstance(getProject());
		try {
			resolvedArtifact = mcBridge.resolveArtifact(cdi, partType);
		}
		catch (Exception e) {
			throw mcBridge.produceContextualizedBuildException("Could not resolve artifact: " + cdi.asString(), e);
		}
		
		if (resolvedArtifact.hasFailed())
			throw mcBridge.produceContextualizedBuildException( resolvedArtifact.getFailure().stringify());
				
		// 
		Part buildPart = resolvedArtifact.getParts().get(partType.asString());

		if (buildPart == null) {
			String msg = String.format( "cannot find part [" + partType.asString() + "] of artifact [%s]", cdi.asString());
			throw mcBridge.produceContextualizedBuildException( msg);
		}
		
		FileResource fileResource = (FileResource)buildPart.getResource();
		
		// set string property 
		getProject().setNewProperty( property, fileResource.getPath());
			
		
	}

	
}
