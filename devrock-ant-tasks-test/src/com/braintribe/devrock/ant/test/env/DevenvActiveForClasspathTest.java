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

import org.junit.Test;

import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests that {@link McBridge} will create the correct data at the correct place
 * if an error is encountered during classpath resolving.
 * 
 * the test case will produce two dump files in the 'processing-data-insight' folder, 
 * one for the failed artifact resolution, the other for the current repository configuration
 * 
 * NOTE: only the existence of the files is tested, *not* the content.
 * 
 * AGAIN: this test can only test the 'dev-env' version of the problem-insight folder. 
 *  
 * @author pit
 *
 */
public class DevenvActiveForClasspathTest extends TaskRunner implements ProcessNotificationListener {
	
	@Override
	protected String filesystemRoot() {	
		return "problem.analysis.devenv.active";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "simplest.failing.tree.definition.yaml");
	}
		

	@Override
	protected void additionalTasks() {
		File devenv = new File( output, "dev-env");
		TestUtils.ensure( devenv);
		// copy structure
		TestUtils.copy( new File(input, "dev-env"), new File(output, "dev-env"));

		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(devenv, "build.xml"));
		TestUtils.copy( new File(input, "pom.xml"), new File(devenv, "pom.xml"));
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
	public void runProblemInsightFolderDeterminationTask() {
		File devenv = new File( output, "dev-env");
		process( new File( devenv, "build.xml"), "dependencies.failing.missing.dep", false, true);
				
		// assert whether the resolution can be found in the problem insight folder
		File location = new File( devenv, "artifacts/processing-data-insight");
		
		Validator validator = new Validator();		
		
		File[] files = location.listFiles();
		validator.assertTrue("no files found in processing-data-insight: " + location.getAbsolutePath(), files != null && files.length > 0);
		if (files != null) {
			validator.assertTrue("expected 2 files in processing-data-insight, but found : " + files.length, files.length == 2);
		}
		File resolutionDump = null;
		File configurationDump = null;
		
		if (files.length == 2) {
			for (int i = 0; i < files.length; i++) {
				File suspect = files[i];
				if (suspect.getName().startsWith( "artifact-resolution-dump")) {
					resolutionDump = suspect;
				}
				else if (suspect.getName().startsWith("repository-configuration-dump")) {
					configurationDump = suspect;
				}
			}
		}
		validator.assertTrue("expected a resolution dump, but found none", resolutionDump != null);
		validator.assertTrue("expected a configuration dump, but found none", configurationDump != null);
		
		validator.assertResults();		
	}
	
		
	
}
