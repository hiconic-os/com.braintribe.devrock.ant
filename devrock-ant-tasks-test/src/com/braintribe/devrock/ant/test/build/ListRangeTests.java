package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests a simple structure for the bt:build-set task.
 *  
 * @author pit
 *
 */
public class ListRangeTests extends TaskRunner {
	
	List<String> foundSequence = new ArrayList<>();

	@Override
	protected String filesystemRoot() {
		return "listRange";
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
		properties.put( "baseDir", ".");
	}

	@Override
	protected void postProcess() {
		File expectedListRangeFile = new File( output, "list-range.txt");		
		foundSequence = readLines(expectedListRangeFile);	
	}

	
	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println(msg);
	}

	@Test
	public void runBuildRangeTests() {
		process( new File( output, "build.xml"), "listRange");
		
		Validator validator = new Validator();
		
		List<String> expectedSequence = new ArrayList<>();
		expectedSequence.add( "com.braintribe.devrock.test:parent#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:z#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:y#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:x#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:b#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:a#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:t#1.0.1");
		
		validator.validateSequence("list-range", foundSequence, expectedSequence);
		
		validator.assertResults();
	}
	
}
