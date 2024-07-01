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
package com.braintribe.devrock.ant.test;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;

import com.braintribe.build.process.ProcessException;
import com.braintribe.build.process.ProcessExecution;
import com.braintribe.build.process.ProcessResults;
import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.common.lcd.Pair;
import com.braintribe.devrock.ant.test.common.HasCommonFilesystemNode;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.ant.test.setup.AntSetterUpper;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.devrock.repolet.generator.RepositoryGenerations;
import com.braintribe.devrock.repolet.launcher.Launcher;
import com.braintribe.devrock.repolet.launcher.LauncherTrait;
import com.braintribe.exception.Exceptions;
import com.braintribe.testing.category.KnownIssue;
import com.braintribe.utils.IOTools;

/**
 * 
 * an abstract runner for ant based tasks .. 
 * 
 * launches the repolet with the given content
 * run@Overrides optional pre-processing tasks 
 * runs the ant tasks as a process
 * runs optional post-processing tasks 
 * stops the repolet again.
 * 
 * requires : 
 * 		a functional ant (in res, i.e. res/ant)
 * 		a deployed bt-ant-tasks to be tested in the ant/lib, i.e. res/ant/lib
 * 
 * NOTE that the ant stuff is automatically pulled if not present. See the {@link AntSetterUpper} for details 
 * 
 * @author pit
 *
 */
@Category(KnownIssue.class)
public abstract class TaskRunner implements LauncherTrait, HasCommonFilesystemNode, HasBase, ProcessNotificationListener {

	protected File repo;
	protected File inst;
	protected File input;
	protected File output;
	protected File antHome;
	protected File uploadFilesystem;
	
	{	
		Pair<File,File> pair = filesystemRoots( filesystemRoot());
		input = pair.first;
		output = pair.second;
		repo = new File( output, "repo");		
		inst = new File( BASE + "/inst");
		
		antHome = new File( "res/ant");
		uploadFilesystem = new File(output, "upload");
	}
	

	protected abstract String filesystemRoot();
	protected abstract RepoletContent archiveContent();
	protected void additionalTasks() {}

	
	protected String antHome() { return antHome.getAbsolutePath();}
	
	protected File settings() { return new File( input.getParentFile(), "settings.xml");}
	
	private Launcher launcher; 
	
	protected Map<String, String> arguments = new HashMap<String, String>();	
	protected Map<String, String> properties = null;
	
	@BeforeClass
	public static void runBeforeClass() {
		AntSetterUpper setterUpper = new AntSetterUpper();
		try {
			setterUpper.prepareTestEnviroment(  new File(BASE));
			//setterUpper.prepareTestEnviroment();
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
	
	protected abstract void preProcess();
	
	protected abstract void postProcess();
	
	protected boolean process(File buildFile, String target) {
		return process( buildFile, target, false, false);
	}
	
	protected boolean process(File buildFile, String target, Map<String,String> envOverrides, Map<String,String> propertyOverrides) {
		return process( buildFile, target, false, false, envOverrides, propertyOverrides);
	}
	
	protected boolean process(File buildFile, String target, boolean activateAntDebug, boolean expectFailure) {
		return process(buildFile, target, activateAntDebug, expectFailure, null, null);
	}
	
	
	protected boolean process(File buildFile, String target, boolean activateAntDebug, boolean expectFailure, Map<String,String> envOverrides, Map<String,String> propertyOverrides) {
		preProcess();
		
		
		// if env overrides have been present, use them, otherwise create a new one
		envOverrides = envOverrides == null ? new HashMap<>() : envOverrides;
		envOverrides.put( "repo", repo.getAbsolutePath());

		//'null' the other properties? 
		envOverrides.put("ARTIFACT_REPOSITORIES_EXCLUSIVE_SETTINGS", settings().getAbsolutePath());
		envOverrides.put( "port", Integer.toString( launcher.getAssignedPort()));
		envOverrides.put( "ANT_HOME", antHome());
		
		if (activateAntDebug) {
			envOverrides.put("ANT_OPTS", "-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y");
		}
		
		boolean success = false;
		try {			
			success = run(Collections.singletonList( target), buildFile, propertyOverrides, envOverrides, this, expectFailure);
		} catch (Exception e) {			
			System.out.println("caught");
		}
				
		postProcess();
		
		return success;
	}
	
	/**
	 * build a list of tokens to be issues to the process runner 
	 * @param buildFile 
	 * @return - a list of the commands for the process runner 
	 */
	private List<String> prepareForRun(File buildFile) {		 
		List<String> result = new ArrayList<String>();
		String home = antHome();
		
		String cmd = home + File.separator + "bin" + File.separator + "ant.bat";
		cmd = cmd.replace( '\\', File.separatorChar);
		result.add( cmd);
		result.add("-f");
		result.add( buildFile.getAbsolutePath());
		
		Map<String,String> combinedMap = new HashMap<String,String>();
		combinedMap.putAll( arguments);
		if (properties != null)
			combinedMap.putAll( properties);
		
		for (String key : combinedMap.keySet()) {
			result.add( "-D" + key + "=" + combinedMap.get(key));
		}							
			
		return result;
	}
	
	protected boolean run( Collection<String> targets, File buildFile, Map<String, String> propertyOverrides, Map<String, String> environmentOverrides, ProcessNotificationListener monitor){
		return run( targets, buildFile, propertyOverrides, environmentOverrides, monitor, false);
	}
	/**
	 * @param targets - the targets to call 
	 * @param directory - the directory to run it 
	 * @param monitor - the {@link ProcessNotificationListener} to be notified 
	 * @throws AntRunException - arrgh
	 */
	protected boolean run( Collection<String> targets, File buildFile, Map<String, String> propertyOverrides, Map<String, String> environmentOverrides, ProcessNotificationListener monitor, boolean expectFailure){
		try {
			List<String> cmd = prepareForRun( buildFile);
			cmd.addAll( targets);
			
			// add any system properties 
			if (propertyOverrides != null) {
				for (Entry<String, String> entry : propertyOverrides.entrySet()) {
					cmd.add( "-D" + entry.getKey() + "=" + entry.getValue());
				}
			}
			  			
			String cl = cmd.stream().collect(Collectors.joining( " "));
			ProcessResults results = ProcessExecution.runCommand( cmd, buildFile.getParentFile(), environmentOverrides, monitor);
			if (results.getRetVal() == 0) {				
				String msg = "Ant task [" + collate(targets) + "] has successfully run";				
				System.out.println(msg);
				if (expectFailure) {
					throw new ProcessException( cl + " -> expected failure, found success");
				}
				return true;
			} else {
				if (!expectFailure) {
					throw new ProcessException( cl + " -> " + results.getErrorText()); 
				}
				return false;
			}		
		} catch (ProcessException e) {
			//		
			String msg = "Cannot run ANT task [" + collate(targets) + "] target as " + e;
			System.out.println(msg);
			if (!expectFailure) { 
				throw new IllegalStateException( msg, e);
			}
			return false;
		}		
		
	}
	
	
	protected String collate(Collection<String> targets) {
		return targets.stream().collect( Collectors.joining(","));
	}
	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {	
		System.out.println(msg);
	}
	
	
	protected List<File> loadFilesFromFilesetDump( File dumpFile) {
		List<File> result = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader( new FileReader( dumpFile))) {
			String line;
			while( (line = reader.readLine()) != null) {
				result.add(new File( line));
			}
			return result;
		}
		catch (Exception e) {
			throw new IllegalStateException( "can't read file [" + dumpFile.getAbsolutePath() + "]", e);
		}
	}
	
	
	protected List<String> extractNamesFromFilesetDump( List<File> files) {
		return files.stream().map( f -> f.getName()).collect(Collectors.toList());
	}
	
	protected List<String> loadNamesFromFilesetDump( File file) {
		return extractNamesFromFilesetDump( loadFilesFromFilesetDump(file));
	}
	
	protected List<String> readLines(File expectedListRangeFile) {
		List<String> result = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader( new FileReader(expectedListRangeFile))){
			String line;
			while ((line = reader.readLine()) != null) {
				result.add( line.trim());
			}
		}
		catch (Exception e) {
			throw Exceptions.uncheckedAndContextualize(e, "Cannot properly read range output file [" + expectedListRangeFile + "]", IllegalStateException::new);
		}
		return result;
	}
	
	protected void copyAndTouchTextFile( File source, File target, Map<String,String> variables) {
		try (
			FileReader fr = new FileReader(source);   //reads the file  
			BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
			PrintWriter pw = new PrintWriter(target);
			){
			String line;  
			while((line=br.readLine())!=null) {
				for (Map.Entry<String, String> entry : variables.entrySet()) {
					String nl = line.replace( entry.getKey(), entry.getValue());
					pw.println( nl);
				}
			}
		}
		catch (Exception e) {
			throw Exceptions.uncheckedAndContextualize(e, "Cannot translate file from [" + source.getAbsolutePath() + "] to [" + target.getAbsolutePath() + "]", IllegalStateException::new);
		}
		
	}
	
	protected String loadTextFile(File file) {
		try {
			return IOTools.slurp( file, "UTF-8");
		} catch (IOException e) {
			throw Exceptions.uncheckedAndContextualize(e, "cannot read file [" + file.getAbsolutePath() + "]", IllegalStateException::new);
		}
	}
	
	protected Map<String, List<String>> produceExpectations(List<String> prefixes, List<String> extensions) {
		Map<String, List<String>> expectations = new HashMap<>();
		
		
		List<String> jars = new ArrayList<>();
		List<String> cp= new ArrayList<>();
		
		for (String prefix : prefixes) {
			for (String extension : extensions) {								
				List<String> values = expectations.computeIfAbsent( extension, k -> new ArrayList<>());
				String filename = prefix + extension;
				values.add( filename);
				if (extension.endsWith(".jar")) {
					// filter out javadoc and sources
					if (extension.startsWith( "-sources") || extension.startsWith("-javadoc")) 
						continue;
					jars.add( filename);
					cp.add( filename);
				}
			}
		}
		
		expectations.put(".jar", jars);
		expectations.put("classpath", cp);
				
		return expectations;
	}

	
	
}
