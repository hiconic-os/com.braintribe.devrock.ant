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

import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * 
 */
public class NoopCandidatePomTaskTest extends TaskRunner {
	
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
		TestUtils.copy( new File(input, "pom.noop.candidate.pc.xml"), new File(output, "pom.xml"));
	}

	@Override
	protected void postProcess() {
		versionedName = loadTextFile( new File( output, "versionedName.txt"));	
	}
	
	@Test
	public void runPomTasksTest() {
		process( new File( output, "build.xml"), "pom");
		
		// assert
		Validator validator = new Validator();		
		validator.validateString(versionedName, "com.braintribe.devrock.test:t#1.0.1-pc");		
		validator.assertResults();
	}

}
