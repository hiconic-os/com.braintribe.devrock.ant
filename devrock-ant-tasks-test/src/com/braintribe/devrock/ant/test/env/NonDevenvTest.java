// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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
