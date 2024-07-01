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
package com.braintribe.devrock.ant.test.plugin;

import java.io.File;
import java.util.List;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests a simple dependencies-task run
 *  
 * @author pit
 *
 */
public class PluginUpdateTaskTest extends TaskRunner implements ProcessNotificationListener {
	
	private List<String> result;

	@Override
	protected String filesystemRoot() {	
		return "plugin.update";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "simplest.tree.definition.yaml");
	}
		

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "update.xml"), new File(output, "update.xml"));
		// copy pom file
		TestUtils.copy( new File(input, "pom.xml"), new File(output, "pom.xml"));
		TestUtils.copy( new File(input, ".classpath"), new File(output, ".classpath"));
		
		TestUtils.copy( new File(input, "META-INF"), new File(output, "META-INF"));
		
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
	public void runSimpleUpdatePluginTask() {
		process( new File( output, "update.xml"), "update-plugin-classpath");
				
		// assert
		Validator validator = new Validator();		
		//validator.validate( "classpath", result, null);
		
		validator.assertResults();		
	}
	
		
	
}
