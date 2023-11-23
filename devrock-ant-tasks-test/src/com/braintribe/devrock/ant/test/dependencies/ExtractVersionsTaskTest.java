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
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.mc.core.declared.DeclaredArtifactIdentificationExtractor;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.utils.IOTools;

public class ExtractVersionsTaskTest extends TaskRunner {

	@Override
	protected String filesystemRoot() {
		return "versions";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}
	
	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
			
		// copy initial files
		TestUtils.copy( new File( input, "initial"), output);		
	}

	@Override
	protected void preProcess() {
		
		properties = new HashMap<>();
		properties.put("range", ".");		
		properties.put( "targetFileName", "out.txt");
		properties.put( "targetDirectoryName", ".");
	}

	@Override
	protected void postProcess() {		
		// grab all 'out.txt' files, and compare with the version inside the pom
		File group = output;
		File [] artifactDirs = group.listFiles( new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {				
				if (pathname.isDirectory())
					return true;
				return false;
			}
		});
		if (artifactDirs == null || artifactDirs.length == 0) {
			Assert.fail("no artifact directories found in [" + output.getAbsolutePath() + "]");
			return;
		}
		boolean failed = false;
		List<String> messages = new ArrayList<>( artifactDirs.length);
		
		for (File artifactDir : artifactDirs) {
			File pom = new File( artifactDir, "pom.xml");
			File versionTxt = new File( artifactDir, "out.txt");
			
			Maybe<CompiledArtifactIdentification> maybe = DeclaredArtifactIdentificationExtractor.extractIdentification(pom);
			if (maybe.isUnsatisfied()) {
				Assert.fail("cannot extract the artifact identification from [" + pom.getAbsolutePath() + "]");
				return;
			}
			CompiledArtifactIdentification cai = maybe.get();
			String versionAsString;
			try {
				versionAsString = IOTools.slurp( versionTxt, Charset.defaultCharset().name());				
			} catch (IOException e) {
				Assert.fail("cannot read the extracted artifact version from [" + versionTxt.getAbsolutePath() + "]");
				return;
			}
			
			String versionInPom = cai.getVersion().asString();
			
			if (versionInPom.compareTo(versionAsString) != 0) {			
				failed = true;
				messages.add( "extraction failed on [" + artifactDir.getAbsolutePath() + "], expected [" + versionInPom + "], extracted [" + versionAsString + "]");
			}						
		}
		
		if (failed) {
			for (String msg : messages) {
				System.out.println(msg);
			}
			Assert.fail();
		}
	}

	@Test
	public void runVersionExtractionTest() {
		process( new File( output, "build.xml"), "extractVersions");
		
		// validate  - see postProcess
	}
}
