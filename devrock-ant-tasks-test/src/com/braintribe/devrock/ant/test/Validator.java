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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.Assert;

import com.braintribe.marshaller.artifact.maven.metadata.DeclaredMavenMetaDataMarshaller;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.artifact.maven.meta.MavenMetaData;
import com.braintribe.model.artifact.maven.meta.Versioning;

/**
 * validator for the bt-ant-tasks-ng tests
 * 
 * @author pit
 *
 */
public class Validator {
	private static final String MAVEN_METADATA_LOCAL_XML = "maven-metadata-local.xml";
	private static final String MAVEN_METADATA_XML = "maven-metadata.xml";
	private List<String> assertionMessages = new ArrayList<>();
	
	/**
	 * simply add the fail message to the list of fail messages if !success
	 * @param failMessage - the message to start 
	 * @param success - if true, nothing happens otherwise the message is stored 
	 */
	public boolean assertTrue( String failMessage, boolean success) {
		if (!success) {
			assertionMessages.add(failMessage);
		}
		return success;
	}
		
	
	/**
	 * asserts the result, i.e. if any assertion messages were accrued, the assertion fails. Clears the messages after call.
	 */
	public void assertResults() {
		if (!assertionMessages.isEmpty()) {
			Assert.fail("assertion failed : \n\t" + assertionMessages.stream().collect(Collectors.joining("\n\t")));
			assertionMessages.clear();
		}		
	}
	
	/**
	 * validates two lists of strings (content of filesets)
	 * @param foundNames
	 * @param expectedNames
	 */
	public void validate(String tag, List<String> foundNames, List<String> expectedNames) {
		List<String> matches = new ArrayList<>( expectedNames.size());
		List<String> excess = new ArrayList<>( expectedNames.size());
		for (String found : foundNames) {
			if (found.length() == 0) {
				assertTrue( "[" + tag + "]: found name is empty", true);
				continue;
			}
			if (expectedNames.contains(found)) {
				matches.add(found);
			}
			else {
				excess.add( found);
			}
		}
		assertTrue( "[" + tag + "]: excess  [" + toString( excess) + "]", excess.size() == 0);
		
		List<String> missing = new ArrayList<>( expectedNames);
		missing.removeAll(matches);		
		assertTrue("[" + tag + "]: missing  [" + toString( missing) + "]", missing.size() == 0);		
	}
	
	private String toString( List<String> strs) {
		return strs.stream().collect(Collectors.joining(","));
	}


	/**
	 * validates the content of an artifact's directory's content
	 * @param repo - the directory 
	 * @param expectedPayloadFiles - a {@link List} 
	 */
	public void validateRepoContent(File repo, List<String> expectedPayloadFiles) {
		File [] files = repo.listFiles();
		
		assertTrue("no files found in expected target [" + repo.getAbsolutePath() + "]", files != null && files.length !=0);
		
		if (files == null || files.length == 0) {
			return;
		}
		
		List<String> matching = new ArrayList<>();
		List<String> excess = new ArrayList<>();
		for (File file : files) {
			String name = file.getName();
			if (expectedPayloadFiles.contains(name)) {
				matching.add( name);
			}
			else {
				excess.add( name);
			}
		}		
		if (excess.contains(MAVEN_METADATA_XML)) {
			excess.remove( MAVEN_METADATA_XML);
		}
		assertTrue( "excess files [" + toString(excess) + "] found", excess.size() == 0);
		
		List<String> missing = new ArrayList<>( expectedPayloadFiles);
		missing.removeAll( matching);
				
		assertTrue( "missing files [" + toString(missing) + "] found", missing.size() == 0);				
	}


	/**
	 * @param location - 
	 * @param artifact - 
	 * @param repositoryId -
	 * @param versions - 
	 */
	public void validateMetadataContent(File location, String artifact, String repositoryId, List<String> versions) {
		File mdFile = repositoryId != null ? new File( location, "maven-metadata-" + repositoryId + ".xml") :new File( location, "maven-metadata.xml");
		if (!mdFile.exists() ) {
			assertTrue("no file [" + mdFile + "] found", false);
			return;
		}
		
		VersionedArtifactIdentification vai = VersionedArtifactIdentification.parse(artifact);
		MavenMetaData metaData;
		try (InputStream in = new FileInputStream(mdFile)) {
			metaData = (MavenMetaData) DeclaredMavenMetaDataMarshaller.INSTANCE.unmarshall(in);
			
		}
		catch( IOException e) {
			fail("cannot unmarshall medata file [" + mdFile.getAbsolutePath() + "]");
			return;
		}
		assertTrue( "expected groupId to be [" + vai.getGroupId() + "], yet found [" + metaData.getGroupId() + "]", vai.getGroupId().equals( metaData.getGroupId()));
		assertTrue( "expected artifactId to be [" + vai.getGroupId() + "], yet found [" + metaData.getGroupId() + "]", vai.getGroupId().equals( metaData.getGroupId()));
	
		Versioning versioning = metaData.getVersioning();
		assertTrue( "no versioning found in [" + mdFile.getAbsolutePath() + "]", versioning != null);
		if (versioning != null) {
			// latest
			assertTrue( "expected latest version to be [" + vai.getVersion() + "], but found [" + versioning.getLatest().asString() + "]", vai.getVersion().equals( versioning.getLatest().asString()));
			
			// list of versions			
			List<String> matching = new ArrayList<>();
			List<String> missing = new ArrayList<>();
			
			List<String> found = versioning.getVersions().stream().map( v -> v.asString()).collect(Collectors.toList());
			
			for (String v : found) {
				if (versions.contains( v)) {
					matching.add( v);
				}
				else {
					missing.add( v);
				}
			}
			List<String> excess = new ArrayList<>( versions);
			excess.removeAll( matching);					
		}
		
	}


	public void validateString(String found, String expected) {
		assertTrue("found range [" + found + "] doesn't match expected range [" + expected +"]", expected.equals( found));
		
	}


	public void validateSequence(String tag, List<String> foundSequence, List<String> expectedSequence) {
		int fl = foundSequence.size();
		int el = expectedSequence.size();
		
		boolean sameSize = assertTrue( tag + ": expected sequence be of length [" + el + "], yet found that it is [" + fl + "]", el == fl);
		if (!sameSize) {
			System.out.println( "expected [" + toString( expectedSequence) + "], yet found [" + toString( foundSequence) + "]");
		}
		
		for (int i = 0; i < fl; i++) {
			String f = foundSequence.get(i);
			String e = expectedSequence.get(i);
			
			assertTrue( tag + ": expected [" + e + "] at position [" + i + "], yet found [" + f + "] there", e.equals(f));
		}
		
	}
	
	public void validatePackageJson(File jsonPackageFile, String expectedVersion) {
		final String VERSION_EXPRESSION_PREFIX = "  \"version\": ";
		final String versionLineRegex = "(\\t| )*\"version\": \"\\d+\\.\\d+\\.\\d+(-.+)?\",";
		int foundVersions = 0;
		try (
				BufferedReader reader = new BufferedReader( new FileReader(jsonPackageFile));
			) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line == null)
					break;
				if (line.matches(versionLineRegex)) {
					foundVersions++;
					String vp = line.substring( VERSION_EXPRESSION_PREFIX.length());
					vp = vp.substring(0, vp.length()-1);
					vp = vp.trim();
					assertTrue( "expected [\"" + expectedVersion + "]\" but found [" + vp + "]", vp.equalsIgnoreCase("\"" + expectedVersion + "\""));									
				} 
			}		
			assertTrue("expected only one version, yet found [" + foundVersions + "] entries", foundVersions == 1);
		} catch (IOException e) {
			throw new IllegalStateException("cannot access file [ " + jsonPackageFile.getAbsolutePath() + "]", e);
		}
	}
	
	public void validateReflectionResult( CompiledArtifactIdentification cai, File folder) {
		if (!assertTrue("Expected folder doesn't exist : " + folder.getAbsolutePath(), folder.exists())) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		for (String string : cai.getArtifactId().split("-")) {			
			sb.append( string.substring(0,1).toUpperCase() + string.substring(1));
		}
		String artifactIdWithoutHyphen = sb.toString();

		File artifactFolder = new File( folder, "artifact");
		
		File subpath = new File( artifactFolder, cai.getGroupId().replace('.', '/'));
		//String name = cai.getArtifactId();
		if (assertTrue("Expected artifact folder doesn't exist: " + subpath.getAbsolutePath(), subpath.exists())) {
			File classFile = new File( subpath, artifactIdWithoutHyphen + ".class");
			assertTrue("Expected class doesn't exist: " + classFile.getName(), classFile.exists());
		}
		
		
		File metaInfoFolder = new File( folder, "META-INF");
		if (assertTrue("Expected META-INF artifact folder doesn't exist: " + metaInfoFolder.getAbsolutePath(), metaInfoFolder.exists())) {
			File propertiesFile = new File( metaInfoFolder, "artifact-descriptor.properties");
			if (assertTrue( "Expected properties file doesn't exist:" + propertiesFile.getAbsolutePath(), propertiesFile.exists())) {
				// read 				
				Properties properties = new Properties();
				try (InputStream in = new FileInputStream(propertiesFile)) {
					properties.load(in);
				}
				catch( IOException e) {					
				}	
				String foundGroupId = properties.getProperty("groupId");
				assertTrue("expected groupId [" + cai.getGroupId() + "] yet found : " + foundGroupId, cai.getGroupId().equals(foundGroupId));
				
				String foundArtifactId = properties.getProperty("artifactId");
				assertTrue("expected artifactId [" + cai.getArtifactId() + "] yet found : " + foundArtifactId, cai.getArtifactId().equals(foundArtifactId));
				
				
				String foundVersion = properties.getProperty("version");
				String expectedVersion = cai.getVersion().asString();
				assertTrue("expected version [" + expectedVersion + "] yet found : " + foundVersion, expectedVersion.equals(foundVersion));
				
				String foundArchetype = properties.getProperty("archetypes");
				assertTrue("expected archetype [" + "model" + "] yet found : " + foundArchetype, "model".equals(foundArchetype));
				
				String foundReflection = properties.getProperty("reflection-class");
				
				
				String expectedReflection = "artifact." + cai.getGroupId() + "." + artifactIdWithoutHyphen; 
				assertTrue("expected reflection [" + expectedReflection + "] yet found : " + foundReflection, expectedReflection.equals( foundReflection));
			}
		}
	}
}
