package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests the 'direct-publish' task 
 * TODO: if bored, add a test that uses the POM task rather than specifying the artifact as a string
 * @author pit
 *
 */
public class DirectPublishTaskTest extends TaskRunner {
	
	private List<String> expectedPayloadFiles = new ArrayList<>();
	{
		expectedPayloadFiles.add( "t-1.0.1.pom");
		expectedPayloadFiles.add( "t-1.0.1.pom.md5");
		expectedPayloadFiles.add( "t-1.0.1.pom.sha1");
		expectedPayloadFiles.add( "t-1.0.1.pom.sha256");
		
		expectedPayloadFiles.add( "t-1.0.1.jar");
		expectedPayloadFiles.add( "t-1.0.1.jar.md5");
		expectedPayloadFiles.add( "t-1.0.1.jar.sha1");
		expectedPayloadFiles.add( "t-1.0.1.jar.sha256");
		
		expectedPayloadFiles.add( "t-1.0.1-sources.jar");
		expectedPayloadFiles.add( "t-1.0.1-sources.jar.md5");
		expectedPayloadFiles.add( "t-1.0.1-sources.jar.sha1");
		expectedPayloadFiles.add( "t-1.0.1-sources.jar.sha256");
		
		expectedPayloadFiles.add( "t-1.0.1-javadoc.jar");
		expectedPayloadFiles.add( "t-1.0.1-javadoc.jar.md5");
		expectedPayloadFiles.add( "t-1.0.1-javadoc.jar.sha1");
		expectedPayloadFiles.add( "t-1.0.1-javadoc.jar.sha256");
		
		expectedPayloadFiles.add( "maven-metadata.xml");
		expectedPayloadFiles.add( "maven-metadata.xml.md5");
		expectedPayloadFiles.add( "maven-metadata.xml.sha1");
		expectedPayloadFiles.add( "maven-metadata.xml.sha256");
		
	}


	@Override
	protected String filesystemRoot() {

		return "deploy";
	}

	@Override
	protected RepoletContent archiveContent() {
		//return archiveInput("deploy.definition.yaml");
		return RepoletContent.T.create();
	}
	
	

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
					
		// copy initial files
		TestUtils.copy( new File( input, "repo"), repo);
	}

	@Override
	protected void preProcess() {		
	}

	@Override
	protected void postProcess() {			
	}
	
	@Test
	public void runDeployTask() {
		process( new File( output, "build.xml"), "deploy", false, false);
				
		// assert
		Validator validator = new Validator();
			
		File uploadedArtifactLocation = new File( uploadFilesystem, "com/braintribe/devrock/test/t");
		File uploadedVersionedArtifactLocation = new File( uploadedArtifactLocation, "1.0.1");
		
		// validate all files 
		validator.validateRepoContent( uploadedVersionedArtifactLocation, expectedPayloadFiles);
		
 		
		validator.assertResults();		
	}

}
