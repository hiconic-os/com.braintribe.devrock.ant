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
package com.braintribe.devrock.ant.test.probing;

import java.io.File;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests that an issue with the probing state of the repository leads to a build exception using dependencies
 * 
 *  
 * @author pit
 *
 */
public class ProbingFailureDependenciesTaskTest extends TaskRunner implements ProcessNotificationListener {
	

	@Override
	protected String filesystemRoot() {	
		return "probing.dependencies";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "simplest.tree.definition.yaml");
	}
	
	@Override
	protected File settings() {
		return new File( input.getParentFile(), "settings.with.probingfailures.xml");
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
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);
		
	}

	@Test
	public void runSimpleDependenciesTasks() {
		process( new File( output, "build.xml"), "dependencies", false, true);								
	}
	
		
	
}
