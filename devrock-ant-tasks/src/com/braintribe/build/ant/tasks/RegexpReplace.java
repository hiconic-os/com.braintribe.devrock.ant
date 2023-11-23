// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Property;

/**
 * replaces a token in a string by a value using regular expressions, set the passed property
 * to the value computed. 
 * 
 * use:
 * <regex property="<propertyToSet>" token="<string to replace in>" expression="<regular expression>" value="<replacement value>" />
 * 
 * @author pit
 *
 */
public class RegexpReplace extends Task {

	String propertyName = null;
	String value = null;
	String expression = null;
	String token = null;
		

	public void setProperty(String property) {
		this.propertyName = property;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public void setToken(String token) {
		this.token = token;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	@Override
	public void execute() throws BuildException {
		
		String replaced = token.replaceAll( expression, value);		
		Property property = new Property();
		property.setProject( getProject());
		property.setName( propertyName);
		property.setValue( replaced);
		property.execute();
		
		super.execute();
	}
	
	
	
}
