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
package com.braintribe.devrock.ant.test.env;

import java.io.File;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 *
 * NON FUNCTIONAL TEST: it can only run if the environment is outside any dev-env. As the JUNIT may
 * run inside a dev-env (for instance, the devrock-ant-tasks-test sits in a dev-env itself), it will 
 * not return the defaulting NON-dev-env, but actually find the dev-env on top and use this location.
 * HENCE this test is not active.
 *  
 * @author pit
 *
 */
public class NonDevenvTest extends TaskRunner implements ProcessNotificationListener {
	
	private String result;
	private String folderFile = "problem-analysis-folder.txt";

	@Override
	protected String filesystemRoot() {	
		return "problem.analysis.devenv";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}
		

	@Override
	protected void additionalTasks() {
		TestUtils.ensure( output);
		
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
	}

	@Override
	protected void preProcess() {
	}

	@Override
	protected void postProcess() {
		result = loadTextFile( new File( output, folderFile));		
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);
		
	}

	//@Test
	public void runNonDevEnvTest() {		
		process( new File( output, "build.xml"), "problem-analysis-folder", false, false);
			
		// assert
		String expectation = new File( output, "artifacts/processing-data-insight").getAbsolutePath();
		String found = result.replace('\\', '/');
		Validator validator = new Validator();		
		validator.assertTrue("expected [" + expectation +"] yet found : " + found, expectation.equals(found));
		
		validator.assertResults();		
	}
	
		
	
}
