// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository.manipulators;

import org.w3c.dom.Element;

import com.braintribe.utils.xml.dom.DomUtils;

/**
 * changes the version of an artifact by changing the version in its pom
 * 
 * @author pit
 *
 */
public class PomRenamer extends AbstractXmlFileManipulator {
	
	private String cloneVersion = null;
	private String groupId = null;
	private String artifactId = null;
	
	public void setVersion(String cloneVersion) {
		this.cloneVersion = cloneVersion;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}


	@Override
	public void adjust() {
		Element documentElement = document.getDocumentElement();
		if (groupId != null) {
			DomUtils.setElementValueByPath( documentElement, "groupId", groupId, false);
		}
		if (artifactId != null) {
			DomUtils.setElementValueByPath( documentElement, "artifactId", artifactId, false);
		}
			
		if (cloneVersion != null) {
			DomUtils.setElementValueByPath( documentElement, "version", cloneVersion, false);			
		}
		
	}

}
