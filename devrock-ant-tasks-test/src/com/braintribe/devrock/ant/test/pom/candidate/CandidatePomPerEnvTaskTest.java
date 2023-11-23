// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package com.braintribe.devrock.ant.test.pom.candidate;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.braintribe.build.ant.tasks.Pom;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests the pom-task (bt:pom) with different settings to get it to generate 'rc'-version suffixes from the read version of the pom
 */
public class CandidatePomPerEnvTaskTest extends TaskRunner {
	
	private String versionedName;
	

	@Override
	protected String filesystemRoot() {		
		return "pom/candidate";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}

	@Override
	protected void preProcess() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		TestUtils.copy( new File(input, "pom.candidate.xml"), new File(output, "pom.xml"));
	}

	@Override
	protected void postProcess() {
		versionedName = loadTextFile( new File( output, "versionedName.txt"));	
	}
	
	/**
	 * triggered by ENV var
	 */
	@Test
	public void runPomTasksPerEnvTest() {
		Map<String,String> env = new HashMap<>();
		env.put(Pom.ensureCandidatePomEnvVariable, "true");
		process( new File( output, "build.xml"), "pom", env, null);
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.2-rc");		
		validator.assertResults();
	}
	
	/**
	 * switched off by ENV var 
	 */
	@Test
	public void runPomTasksPerEnvOffTest() {
		Map<String,String> env = new HashMap<>();
		env.put(Pom.ensureCandidatePomEnvVariable, "false");
		process( new File( output, "build.xml"), "pom", env, null);
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.1");		
		validator.assertResults();
	}
	
	/**
	 * triggered by property 
	 */
	@Test
	public void runPomTasksPerPropertyTest() {
		Map<String,String> properties = new HashMap<>();
		properties.put(Pom.ensureCandidatePomProperty, "true");
		process( new File( output, "build.xml"), "pom", null, properties);
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.2-rc");		
		validator.assertResults();
	}
	
	/**
	 * switched of by property 
	 */
	@Test
	public void runPomTasksPerPropertyOffTest() {
		Map<String,String> properties = new HashMap<>();
		properties.put(Pom.ensureCandidatePomProperty, "false");
		process( new File( output, "build.xml"), "pom", null, properties);
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.1");		
		validator.assertResults();
	}

	
	/**
	 * overriding ENV var by the property w
	 */
	@Test
	public void runPomTasksPerPropertyOverrideTest() {
		Map<String,String> properties = new HashMap<>();
		properties.put(Pom.ensureCandidatePomProperty, "true");
		
		Map<String,String> env = new HashMap<>();
		env.put(Pom.ensureCandidatePomEnvVariable, "false");		
		
		process( new File( output, "build.xml"), "pom", env, properties);
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.2-rc");		
		validator.assertResults();
	}

	
	/**
	 * triggered as default
	 */
	@Test
	public void runPomTasksPerDefaultTest() {
		process( new File( output, "build.xml"), "pom", null, null);
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.2-rc");		
		validator.assertResults();
	}


}
