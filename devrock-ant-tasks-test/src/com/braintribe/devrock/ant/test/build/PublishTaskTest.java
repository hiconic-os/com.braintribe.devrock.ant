package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests the publish tasks 
 * with increasing the version, yet no GIT interaction
 *  
 * @author pit
 *
 */

// TODO: doesn't test overwriting the metadata.xml of the unversioned artifact.
// Two reasons:
// a) currently active version of this task doesn't overwrite either
// b) the repolet doesn't allow deletion of a a file that is part of its content, only files 
// that have been uploaded in the first place can be deleted (can't delete files that only exist as description)
// 

public class PublishTaskTest extends TaskRunner implements ProcessNotificationListener {
	
	private List<String> expectedPayloadFiles = new ArrayList<>();
	{
		expectedPayloadFiles.add( "t-1.0.2.pom");
		expectedPayloadFiles.add( "t-1.0.2.pom.md5");
		expectedPayloadFiles.add( "t-1.0.2.pom.sha1");
		expectedPayloadFiles.add( "t-1.0.2.pom.sha256");
		
		expectedPayloadFiles.add( "t-1.0.2.jar");
		expectedPayloadFiles.add( "t-1.0.2.jar.md5");
		expectedPayloadFiles.add( "t-1.0.2.jar.sha1");
		expectedPayloadFiles.add( "t-1.0.2.jar.sha256");
		
		expectedPayloadFiles.add( "t-1.0.2-sources.jar");
		expectedPayloadFiles.add( "t-1.0.2-sources.jar.md5");
		expectedPayloadFiles.add( "t-1.0.2-sources.jar.sha1");
		expectedPayloadFiles.add( "t-1.0.2-sources.jar.sha256");
		
		expectedPayloadFiles.add( "t-1.0.2-javadoc.jar");
		expectedPayloadFiles.add( "t-1.0.2-javadoc.jar.md5");
		expectedPayloadFiles.add( "t-1.0.2-javadoc.jar.sha1");
		expectedPayloadFiles.add( "t-1.0.2-javadoc.jar.sha256");
		
		expectedPayloadFiles.add( "maven-metadata.xml");
		expectedPayloadFiles.add( "maven-metadata.xml.md5");
		expectedPayloadFiles.add( "maven-metadata.xml.sha1");
		expectedPayloadFiles.add( "maven-metadata.xml.sha256");
		
		expectedPayloadFiles.add( "package.json");
		expectedPayloadFiles.add( "package.json.md5");
		expectedPayloadFiles.add( "package.json.sha1");
		expectedPayloadFiles.add( "package.json.sha256");
		
	}

	@Override
	protected String filesystemRoot() {	
		return "publish";
	}

	@Override
	protected RepoletContent archiveContent() {
			return archiveInput( "publish.definition.yaml");
	}
		

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
			
		// copy initial files
		TestUtils.copy( new File( input, "initial"), output);		
	}

	@Override
	protected void preProcess() {}

	@Override
	protected void postProcess() {}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println( msg);		
	}
	

	@Test
	public void runPublishTask() {
		process( new File( output, "build.xml"), "publish", false, false);
				
		// assert
		Validator validator = new Validator();
			
		File localArtifactLocation = new File( uploadFilesystem, "com/braintribe/devrock/test/t");
		File localVersionedArtifactLocation = new File( localArtifactLocation, "1.0.2");
					
		validator.validateRepoContent( localVersionedArtifactLocation, expectedPayloadFiles);
		/*
		validator.validateMetadataContent(localArtifactLocation, "com.braintribe.devrock.test:t#1.0.1", null, Collections.singletonList( "1.0.2"));
		*/
	
		File jsonFile = new File( uploadFilesystem, "com/braintribe/devrock/test/t/1.0.2/package.json");		
		if (jsonFile != null && jsonFile.exists()) {
			validator.validatePackageJson(jsonFile, "1.0.2");
		}
	
		
		validator.assertResults();		
	}
	
	//@Test
	// deactivated for testing with bt-ant-tasks-ng
	public void runPublishTaskWithOverwrite() {
		process( new File( output, "build.xml"), "publishWithOverwrite");
				
		// assert
		Validator validator = new Validator();
			
		File localArtifactLocation = new File( uploadFilesystem, "com/braintribe/devrock/test/t");
		File localVersionedArtifactLocation = new File( localArtifactLocation, "1.0.2");
					
		validator.validateRepoContent( localVersionedArtifactLocation, expectedPayloadFiles);
		
		validator.validateMetadataContent(localArtifactLocation, "com.braintribe.devrock.test:t#1.0.2", null, Collections.singletonList( "1.0.2"));
		
		File jsonFile = new File( uploadFilesystem, "com/braintribe/devrock/test/t/1.0.2/package.json");		
		if (jsonFile != null && jsonFile.exists()) {
			validator.validatePackageJson(jsonFile, "1.0.2");
		}
	
		validator.assertResults();		
	}
	
	@Test
	public void runPublishTaskWithJson() {
		process( new File( output, "build.xml"), "publishWithJson");
				
		// assert
		Validator validator = new Validator();
			
		File localArtifactLocation = new File( uploadFilesystem, "com/braintribe/devrock/test/t");
		File localVersionedArtifactLocation = new File( localArtifactLocation, "1.0.2");
					
		validator.validateRepoContent( localVersionedArtifactLocation, expectedPayloadFiles);
		
		File jsonFile = new File( uploadFilesystem, "com/braintribe/devrock/test/t/1.0.2/package.json");		
		if (jsonFile != null && jsonFile.exists()) {
			validator.validatePackageJson(jsonFile, "1.0.2");
		}
	
		validator.assertResults();		
	}
		
	
}
