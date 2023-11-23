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
package com.braintribe.devrock.ant.test.direct.hasher;

import java.io.File;
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.experimental.categories.Category;

import com.braintribe.build.ant.mc.DirectMcBridge;
import com.braintribe.devrock.ant.test.RepoletRunner;
import com.braintribe.devrock.ant.test.common.HasCommonFilesystemNode;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.testing.category.KnownIssue;

// TODO : will create a ${env.repo} directory in the main artifact as settings.xml is not prepared for this test. 
// deactivated for now
// 
// not to be run by the CI 
@Category( KnownIssue.class)
public class DirectSolutionHasherTest extends RepoletRunner implements HasCommonFilesystemNode {
	private static final String TERMINAL = "com.braintribe.devrock.test:t#1.0.1";

	@Override
	protected String filesystemRoot() {
		return "hasher";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput( "parent_and_import.archive.definition.yaml"); 
	}
	
	
	
	@Override
	protected File settings() {				
		return super.settings();
	}

	//@Test
	public void runDirectSolutionHasherTest() {
		
		// TODO : must be a proper settings xml 
		DirectMcBridge mcBridge = new DirectMcBridge(settings(), launcher.getAssignedPort());
		
		CompiledTerminal ct = CompiledTerminal.from( VersionedArtifactIdentification.parse(TERMINAL));
		AnalysisArtifactResolution resolution = mcBridge.resolveClashfreeRelevantSolutions(Collections.singletonList(ct), null, null, null, null);
		
	
		mcBridge.writeResolutionToFile(resolution, new File( output, "dump.yaml"));
	
		System.out.println( resolution.getSolutions().stream().map( s -> s.asString()).collect(Collectors.joining(",")));
		mcBridge.close();
	}
	
	

}
