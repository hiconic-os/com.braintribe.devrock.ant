// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.utils.property;

import org.apache.tools.ant.Project;

/**
 * some magic for velocity - required to make sure that the property is 
 * only accessed after the full path has been created.<br/>
 * 
 * @author pit
 *
 */
public class DeferredAccessor {
	 private String key;
	 private Project project;
	
	
	public DeferredAccessor(Project project, String key) {
	   this.key = key;
	   this.project = project;
	  }
	  
	  public Object get(String key) {
	   String accumulatedKey = this.key + "." + key;
	   return new DeferredAccessor(project, accumulatedKey);
	  }
	  
	  @Override
	  public String toString() {		
		  String returnValue = project.getProperty(key);
		  return returnValue;
	  }
	  
	  
	  
}
