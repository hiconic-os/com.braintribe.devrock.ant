// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.types;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectComponent;

/**
 * 
 * <authentication username="builder" password="operating2005"/>
 * 
 * @author pit
 *
 */
public class Authentication extends ProjectComponent {

	private String id;
	private String refid;
	
	private String username;
	private String password;
	
	private Authentication sibling;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {		
		this.id = id;
		Project project = getProject();
		if (project == null)
			return;
		project.addReference( id, this);
	}
	public String getRefid() {
		return refid;
	}
	public void setRefid(String refid) {
		this.refid = refid;						
	}
	public String getUsername() {
		if (username != null)
			return username;
		Authentication sibling = getSibling();
		if (sibling != null)
			return sibling.getUsername();
		return null;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		if (password != null)
			return password;
		Authentication sibling = getSibling();
		if (sibling != null)
			return sibling.getPassword();
		return null;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	
	private Authentication getSibling() {
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
		if (obj instanceof Authentication == false)
			return null;
		sibling = (Authentication) obj;
		return sibling;
	}
	
}
