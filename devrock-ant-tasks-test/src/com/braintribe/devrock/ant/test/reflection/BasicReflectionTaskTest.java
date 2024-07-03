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
package com.braintribe.devrock.ant.test.reflection;

import java.io.File;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;

/**
 * tests the reflection building task - in a positive case, i.e. the test checks a correct run.
 *  
 * @author pit
 *
 */
public class BasicReflectionTaskTest extends TaskRunner implements ProcessNotificationListener {	

	@Override
	protected String filesystemRoot() {	
		return "reflection.basic";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}
		

	@Override
	protected void additionalTasks() {
		// copy test artifact
		TestUtils.copy( new File(input, "initial"), output);
	}

	@Override
	protected void preProcess() {
	}

	@Override
	protected void postProcess() {
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);
		
	}

	@Test
	public void runReflectionTask() {
		process( new File( output, "build.xml"), "reflection", false, false);
				
		// assert
		Validator validator = new Validator();
		
		File cgen = new File( output, "class-gen");
		
		validator.validateReflectionResult(CompiledArtifactIdentification.parse("com.braintribe.devrock.test:test-artifact-model#2.0.4-pc"), cgen);
		
		validator.assertResults();		
	}
	
		
	
}
