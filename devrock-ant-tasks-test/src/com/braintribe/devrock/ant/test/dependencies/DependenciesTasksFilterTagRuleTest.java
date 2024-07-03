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
package com.braintribe.devrock.ant.test.dependencies;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests multiple tag rules 
 * 
 * @author pit
 *
 */
public class DependenciesTasksFilterTagRuleTest extends TaskRunner {
	private List<String> result;

	@Override
	protected String filesystemRoot() {	
		return "dependencies.filter";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "archive.definition.yaml");
	}
		

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		// copy pom file
		TestUtils.copy( new File(input, "pom.xml"), new File(output, "pom.xml"));
	}

	@Override
	protected void preProcess() {
	}

	@Override
	protected void postProcess() {
		result = loadNamesFromFilesetDump( new File(output, "classpath.txt"));
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);
		
	}

	@Test
	public void runDefaultFilter() {
		process( new File( output, "build.xml"), "dependencies.default", false, false);
		
		List<String> expectations = new ArrayList<>();					
		expectations.add("none-1.0.1.jar");			
		expectations.add("classpath-1.0.1.jar");
		expectations.add("standard-1.0.1.jar");
		expectations.add("one-1.0.1.jar");
		expectations.add("one-and-two-1.0.1.jar");								
							
		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
	
	@Test
	public void runAllInFilter() {
		process( new File( output, "build.xml"), "dependencies.allin", false, false);
		
		List<String> expectations = new ArrayList<>();								
		expectations.add("classpath-1.0.1.jar");
		expectations.add("standard-1.0.1.jar");
		expectations.add("one-1.0.1.jar");
		expectations.add("one-and-two-1.0.1.jar");								
							
		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
	@Test
	public void runAllOutFilter() {
		process( new File( output, "build.xml"), "dependencies.allout", false, false);
		
		List<String> expectations = new ArrayList<>();								
		expectations.add("classpath-1.0.1.jar");
		expectations.add("none-1.0.1.jar");

		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
	@Test
	public void runOneFilter() {
		process( new File( output, "build.xml"), "dependencies.one", false, false);
		
		List<String> expectations = new ArrayList<>();								
		expectations.add("one-1.0.1.jar");
		expectations.add("one-and-two-1.0.1.jar");								

		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
	@Test
	public void runOneAndTwoFilter() {
		process( new File( output, "build.xml"), "dependencies.oneAndTwo", false, false);
		
		List<String> expectations = new ArrayList<>();								
		expectations.add("one-and-two-1.0.1.jar");								

		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
	@Test
	public void runOneNotTwoFilter() {
		process( new File( output, "build.xml"), "dependencies.oneNotTwo", false, false);
		
		List<String> expectations = new ArrayList<>();								
		expectations.add("one-1.0.1.jar");								

		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
	@Test
	public void runNeitherTwoNorStandardFilter() {
		process( new File( output, "build.xml"), "dependencies.neitherTwoNorStandard", false, false);
		
		List<String> expectations = new ArrayList<>();								
		expectations.add("none-1.0.1.jar");			
		expectations.add("classpath-1.0.1.jar");		
		expectations.add("one-1.0.1.jar");											

		// assert
		Validator validator = new Validator();		
		validator.validate( "classpath", result, expectations);
		
		validator.assertResults();		
	}
}
