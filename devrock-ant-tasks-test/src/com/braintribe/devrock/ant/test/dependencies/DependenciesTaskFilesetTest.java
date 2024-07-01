// ============================================================================
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
package com.braintribe.devrock.ant.test.dependencies;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests a simple dependencies-task run with multiple filesets
 *  
 * @author pit
 *
 */
public class DependenciesTaskFilesetTest extends TaskRunner implements ProcessNotificationListener {
	
	private Map<String, List<String>> retrieved = new HashMap<>();

	@Override
	protected String filesystemRoot() {	
		return "dependencies.fileset";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "simplest.tree.definition.yaml");
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
		retrieved.put( "classpath", loadNamesFromFilesetDump( new File(output, "classpath.txt")));
		retrieved.put( ".jar", loadNamesFromFilesetDump( new File(output, "jars.txt")));
		retrieved.put( ".pom", loadNamesFromFilesetDump( new File(output, "poms.txt")));
		retrieved.put( "-sources.jar", loadNamesFromFilesetDump( new File(output, "sources.txt")));
		retrieved.put( "-javadoc.jar", loadNamesFromFilesetDump( new File(output, "javadocs.txt")));
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);
		
	}
	
	
	@Test
	public void runFilesetDependenciesTasks() {
		process( new File( output, "build.xml"), "dependencies");
		
				

		List<String> prefixes = new ArrayList<>();
		prefixes.add( "a-1.0.1");
		prefixes.add( "b-1.0.1");
		prefixes.add( "t-1.0.1");
		prefixes.add( "x-1.0.1");
		prefixes.add( "y-1.0.1");
		prefixes.add( "z-1.0.1");		
		
		List<String> extensions = new ArrayList<>();
		extensions.add(".jar");
		extensions.add(".pom");
		extensions.add("-sources.jar");
		extensions.add("-javadoc.jar");
		
		Map<String, List<String>> expectations = produceExpectations(prefixes, extensions);
		// 
		
		
		// assert
		Validator validator = new Validator();
		
		for (Map.Entry<String, List<String>> entry : retrieved.entrySet()) {		
			validator.validate( entry.getKey(), entry.getValue(), expectations.get( entry.getKey()));
		}
		
		
		validator.assertResults();		
	}
	
		
	
}
