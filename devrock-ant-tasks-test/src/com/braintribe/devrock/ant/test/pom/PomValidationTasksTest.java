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
package com.braintribe.devrock.ant.test.pom;

import java.io.File;

import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

public class PomValidationTasksTest extends TaskRunner {
	
	

	@Override
	protected String filesystemRoot() {		
		return "pom/validate";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}

	@Override
	protected void preProcess() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		TestUtils.copy( new File(input, "valid.pom.xml"), new File(output, "valid.pom.xml"));
		TestUtils.copy( new File(input, "semantically.invalid.pom.xml"), new File(output, "semantically.invalid.pom.xml"));
		TestUtils.copy( new File(input, "syntactically.invalid.pom.xml"), new File(output, "syntactically.invalid.pom.xml"));
	}

	@Override
	protected void postProcess() {}
	
	@Test
	public void runValidPomTasksTest() {
		process( new File( output, "build.xml"), "pom");				
	}
	
	@Test
	public void runSyntacticallyInvalidPomTasksTest() {
		process( new File( output, "build.xml"), "syntactically.invalid.pom", false, true);				
	}
	@Test
	public void runSemanticallyInvalidPomTasksTest() {
		process( new File( output, "build.xml"), "semantically.invalid.pom", false, true);				
	}


}
