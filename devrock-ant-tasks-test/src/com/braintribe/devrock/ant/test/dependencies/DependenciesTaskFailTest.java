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
import java.io.FileFilter;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests failures to see that the active repository configuration is dumped by the {@link McBridge}
 *  
 * @author pit
 *
 */
public class DependenciesTaskFailTest extends TaskRunner implements ProcessNotificationListener {
	
	private File dumped;
	

	@Override
	protected String filesystemRoot() {	
		return "dependencies.failing";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "simplest.failing.tree.definition.yaml");
	}
		

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		// copy pom file
		TestUtils.copy( new File(input, "valid.pom.xml"), new File(output, "valid.pom.xml"));
		TestUtils.copy( new File(input, "missing.parent.invalid.pom.xml"), new File(output, "missing.parent.invalid.pom.xml"));
	}

	@Override
	protected void preProcess() {
	}

	@Override
	protected void postProcess() {
		dumped = findYamlFile();
		
		
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);
		
	}
	
	private File findYamlFile() {
		File[] files = output.listFiles( new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				if (!pathname.getAbsolutePath().endsWith(".yaml")) 
					return true;				
				return false;
				
			}
		});
		
		if (files.length > 0) {
			return files[0];				
		}
		return null;
		
	}

	@Test
	public void runSimpleFailingDependenciesTask() {
		process( new File( output, "build.xml"), "dependencies.failing.missing.dep", false, true);
		// 
		Assert.assertTrue("no dump file found", dumped != null);
			
	}
	@Test
	public void runSimpleFailingParentTask() {
		process( new File( output, "build.xml"), "dependencies.failing.missing.parent", false, true);
		Assert.assertTrue("no dump file found", dumped != null);
			
	}
		
	
}
