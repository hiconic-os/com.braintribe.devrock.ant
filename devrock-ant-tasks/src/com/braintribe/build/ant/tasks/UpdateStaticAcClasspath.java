// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.util.Set;

import org.apache.tools.ant.BuildException;

/**
 * overloads standard UpdateEclipseClasspath by using a different name
 * for the classpath file -> used by AC 2.0
 * 
 * @author pit
 *
 */
public class UpdateStaticAcClasspath extends UpdateEclipseClasspath {

	@Override
	protected void updateClasspathFile(Set<ClasspathEntry> entries) throws BuildException {

		super.updateClasspathFile(entries, ".static.classpath");
	}

}
