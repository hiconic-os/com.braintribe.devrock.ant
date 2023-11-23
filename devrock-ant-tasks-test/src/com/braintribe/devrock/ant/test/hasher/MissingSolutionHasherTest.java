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
package com.braintribe.devrock.ant.test.hasher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;

import com.braintribe.build.ant.tasks.SolutionHasher;
import com.braintribe.build.process.listener.MessageType;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.utils.IOTools;

/**
 * tests how the solution hasher reacts to missing solutions
 * 
 * @author pit
 *
 */
public class MissingSolutionHasherTest extends TaskRunner {
	
	private static final String HASH_COMMON = "hash.txt";
	private static final String HASH_TEST_GROUP = "com.braintribe.devrock.test.hash.txt";
	private static final String HASH_GROUPS = "groups.hash.txt";
	private static final String EXPECTED_RESOLUTION_SUFFIX = ".yaml";
	private List<String> expectedFiles;	
	{
		expectedFiles = new ArrayList<>();
		expectedFiles.add( HASH_COMMON);
		expectedFiles.add( HASH_GROUPS);
		expectedFiles.add( HASH_TEST_GROUP);
	}
	
	private List<String> expectedGroups;
	{
		expectedGroups = new ArrayList<>();
		expectedGroups.add( "com.braintribe.devrock.test");
	}
	
		
	private List<String> expectedArtifacts;
	{
		expectedArtifacts = new ArrayList<>();
		expectedArtifacts.add( "com.braintribe.devrock.test:a#1.0.1");
		expectedArtifacts.add( "com.braintribe.devrock.test:b#1.0.1");
		expectedArtifacts.add( "com.braintribe.devrock.test:t#1.0.1");
		expectedArtifacts.add( "com.braintribe.devrock.test:parent#1.0.1");
	}
	
	private List<String> expectedDumps;
	{
		expectedDumps = new ArrayList<>();
		expectedDumps.add( "com.braintribe.devrock.test.a#1.0.1.resolution" + EXPECTED_RESOLUTION_SUFFIX);
		expectedDumps.add( "com.braintribe.devrock.test.b#1.0.1.resolution" + EXPECTED_RESOLUTION_SUFFIX);
		expectedDumps.add( "com.braintribe.devrock.test.t#1.0.1.resolution" + EXPECTED_RESOLUTION_SUFFIX);
		expectedDumps.add( "com.braintribe.devrock.test.parent#1.0.1.resolution" + EXPECTED_RESOLUTION_SUFFIX);
	}

	
	

	@Override
	protected String filesystemRoot() {
		return "hasher-missing";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput("archive.definition.yaml");
	}

	@Override
	protected void preProcess() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		// copy initial 
		TestUtils.copy( new File( input, "initial"), output);		
	}

	@Override
	protected void postProcess() {
		// collect hash files		
	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);		
	}
	
	@Test
	public void runHashingTest() {
		Map<String,String> propertyOverrides = new HashMap<>();
		propertyOverrides.put( "basedir", output.getAbsolutePath());
		propertyOverrides.put( "range", ".");
		process( new File( output, "build.xml"), "solutionsHash", false, false, null, propertyOverrides);
	
		/*
		 * 3 files
		 * a) hash.txt -> single hash entry 
		 * b) groups.hash.txt -> "com.braintribe.devrock.test <hash>"  
		 * c) com.braintribe.devrock.test.hash.txt -> "<qualified artifact> <hash>[\n..]"   
		 */
		
		Validator validator = new Validator();
		// existence of the files
		for (String name : expectedFiles) {
			validator.assertTrue( "file [" + name + "] expected, but not found", new File( output, name).exists());
		}
		
		for (String name : expectedDumps) {
			validator.assertTrue( "dump file [" + name + "] expected, but not found", new File( output, name).exists());	
		}
		
		// contents of the group hash file
		try {
			String contents = IOTools.slurp( new File( output, HASH_TEST_GROUP), "UTF-8");
			String [] groups = contents.split("\n");
			if (groups == null) {
				validator.assertTrue("no content found in [" + HASH_TEST_GROUP + "]", false);				
			}
			else {
				List<String> matches = new ArrayList<>();
				List<String> excess = new ArrayList<>();
				for (String group : groups) {
					String line = group.trim();
					String groupName = line.substring(0, line.indexOf( ' '));
					if (expectedArtifacts.contains(groupName)) {
						matches.add(groupName);
					}
					else {
						excess.add(groupName);
					}
				}
				List<String> missing = new ArrayList<>( expectedArtifacts);
				missing.removeAll(matches);
				
				validator.assertTrue("excess artifacts [" + excess.stream().collect(Collectors.joining(",")), excess.size() == 0);
				validator.assertTrue("missing artifacts [" + missing.stream().collect(Collectors.joining(",")), missing.size() == 0);
			}
		
		} catch (IOException e) {
			validator.assertTrue("cannot open file [" + HASH_GROUPS + "]", false);
		}
		
		// contents of the file for the com.braintribe.devrock.test group
		try {
			String contents = IOTools.slurp( new File( output, HASH_GROUPS), "UTF-8");
			String [] groups = contents.split("\n");
			if (groups == null) {
				validator.assertTrue("no content found in [" + HASH_GROUPS + "]", false);				
			}
			else {
				List<String> matches = new ArrayList<>();
				List<String> excess = new ArrayList<>();
				for (String group : groups) {
					String line = group.trim();
					String groupName = line.substring(0, line.indexOf( ' '));
					if (expectedGroups.contains(groupName)) {
						matches.add(groupName);
					}
					else {
						excess.add(groupName);
					}
				}
				List<String> missing = new ArrayList<>( expectedGroups);
				missing.removeAll(matches);
				
				validator.assertTrue("excess groups [" + excess.stream().collect(Collectors.joining(",")), excess.size() == 0);
				validator.assertTrue("missing groups [" + missing.stream().collect(Collectors.joining(",")), missing.size() == 0);
			}
		
		} catch (IOException e) {
			validator.assertTrue("cannot open file [" + HASH_GROUPS + "]", false);
		}
				
		File missingSolutionsFile = new File( output, SolutionHasher.MISSING_SOLUTIONS_FILE_NAME);
		validator.assertTrue( "no " + SolutionHasher.MISSING_SOLUTIONS_FILE_NAME + " file found", missingSolutionsFile.exists());
		
		try {
			String contents = IOTools.slurp( missingSolutionsFile, "UTF-8");
			String [] missings = contents.split("\n");
			
			validator.assertTrue("expected [1] entry, but found [" + missings.length + "]", missings.length == 1);
			
			String foundMissingSolution = missings[0];
			String expectedMissing = "com.braintribe.devrock.test:missing#[1.0,1.1)";			
			validator.assertTrue("expected unresolved dependency to be [" + expectedMissing + "], yet found [" + foundMissingSolution + "]", expectedMissing.equals(foundMissingSolution));
			
		} catch (IOException e) {
			validator.assertTrue("cannot open file [" + SolutionHasher.MISSING_SOLUTIONS_FILE_NAME + "]", false);
		}
		
		
		
		
		validator.assertResults();
				
	}
}
