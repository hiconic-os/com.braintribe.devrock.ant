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
 * tests the install tasks 
 *  
 * @author pit
 *
 */
public class InstallTaskTest extends TaskRunner implements ProcessNotificationListener {

	@Override
	protected String filesystemRoot() {	
		return "install";
	}

	@Override
	protected RepoletContent archiveContent() {
		// not required actually 
		return RepoletContent.T.create();
	}
		

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
			
		// copy initial files
		TestUtils.copy( new File( input, "initial/release"), output);		
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
	public void runInstallTasks() {
		process( new File( output, "build.xml"), "install");
				
		// assert
		Validator validator = new Validator();
		
		List<String> expectedPayloadFiles = new ArrayList<>();
		expectedPayloadFiles.add( "t-1.0.1.pom");
		expectedPayloadFiles.add( "t-1.0.1.jar");
		expectedPayloadFiles.add( "t-1.0.1-sources.jar");
		expectedPayloadFiles.add( "t-1.0.1-javadoc.jar");
		
		File localArtifactLocation = new File( inst, "com/braintribe/devrock/test/t");
		File localVersionedArtifactLocation = new File( localArtifactLocation, "1.0.1");
					
		validator.validateRepoContent( localVersionedArtifactLocation, expectedPayloadFiles);
		validator.validateMetadataContent(localArtifactLocation, "com.braintribe.devrock.test:t#1.0.1", null, Collections.singletonList( "1.0.1"));
		
		validator.assertResults();		
	}
	
		
	
}
