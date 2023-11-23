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
 * renames a project by changing its .project file
 * @author pit
 *
 */
public class DotProjectRenamer extends AbstractXmlFileManipulator {

	private String projectName = null;
	
	
	
	public String getProjectName() {
		return projectName;
	}



	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}


	@Override
	public void adjust() {
		Element documentElement = document.getDocumentElement();
		Element versionElement = DomUtils.getElementByPath(documentElement, "name", false);
		if (versionElement != null) {
			versionElement.setTextContent( projectName);
		}
	}

}
