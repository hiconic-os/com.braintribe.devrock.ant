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
package com.braintribe.devrock.ant.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.braintribe.common.lcd.Pair;
import com.braintribe.devrock.ant.test.common.HasCommonFilesystemNode;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.ant.test.setup.AntSetterUpper;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.devrock.repolet.generator.RepositoryGenerations;
import com.braintribe.devrock.repolet.launcher.Launcher;
import com.braintribe.devrock.repolet.launcher.LauncherTrait;
import com.braintribe.exception.Exceptions;

/**
 * an abstract runner for simple tests that do require the repolet, yet not ant 
 * @author pit
 *
 */
public abstract class RepoletRunner implements LauncherTrait, HasCommonFilesystemNode, HasBase {
	protected File repo;
	protected File input;
	protected File output;
	protected File uploadFilesystem;	
	{	
		Pair<File,File> pair = filesystemRoots( filesystemRoot());
		input = pair.first;
		output = pair.second;
		repo = new File( output, "repo");
		
		uploadFilesystem = new File(output, "upload");
	}
	

	/**
	 * @return - the root directory for the test's file system
	 */
	protected abstract String filesystemRoot();
	/**
	 * @return - the {@link RepoletContent} to be used 
	 */
	protected abstract RepoletContent archiveContent();

	protected File settings() { return new File( input.getParentFile(), "settings.xml");}
	protected void additionalTasks() {}
	
	protected Launcher launcher; 
	
	protected Map<String, String> arguments = new HashMap<String, String>();	
	protected Map<String, String> properties = null;
	
	@BeforeClass
	public static void runBeforeClass() {
		AntSetterUpper setterUpper = new AntSetterUpper();
		try {
			setterUpper.prepareTestEnviroment( new File(BASE));
		} catch (Exception e) {
			fail("setup failed : " + e.getMessage());
		}		
	}

	
	@Before
	public void runBefore() {	
		TestUtils.ensure(output); 			
		additionalTasks();
		
		launcher = Launcher.build()
				.repolet()
				.name("archive")
					.descriptiveContent()
						.descriptiveContent(archiveContent())
					.close()
					.uploadFilesystem()
						.filesystem( uploadFilesystem)
					.close()
				.close()
			.done();

		launcher.launch();		
	}
	
	@After
	public void runAfter() {
		launcher.shutdown();
	}
	
	protected RepoletContent archiveInput(File file) {	
		try {
			return RepositoryGenerations.unmarshallConfigurationFile(file);
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "cannot load parser file [" + file.getAbsolutePath() + "]" , IllegalStateException::new);
		} 
	}
	protected RepoletContent archiveInput(String definition) {
		File file = new File( input, definition);
		try {
			return RepositoryGenerations.unmarshallConfigurationFile(file);
		} catch (Exception e) {
			throw Exceptions.unchecked(e, "cannot load parser file [" + file.getAbsolutePath() + "]" , IllegalStateException::new);
		} 
	}
	
}
