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
 * tests the special 'range' syntax of ']]a+y'.
 * 
 * t
 * 	a
 * 		b
 * 	x
 * 		y
 * 			z
 * 
 * expected in order : y,x,a,t
 * 
 * if 'range' is ']]b+z', expected is z,y,x, b,a,t
 *  
 *  
 * branches x->y->z and b->a are disjunct, they can be swapped in order. This here test the current order. 
 *  
 * @author pit
 *
 */
public class ListSpecialRangeTests extends TaskRunner {
	
	List<String> foundSequence = new ArrayList<>();

	@Override
	protected String filesystemRoot() {
		return "listSpecialRange";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}
	

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		TestUtils.copy( new File(input, "build.2.xml"), new File(output, "build.2.xml"));
		// copy initial files
		TestUtils.copy( new File( input, "initial"), output);		
	}

	@Override
	protected void preProcess() {
		properties = new HashMap<>();
		//properties.put("range", ".");		
		properties.put( "baseDir", ".");
	}

	@Override
	protected void postProcess() {
		File expectedListRangeFile = new File( output, "list-range.txt");		
		if (expectedListRangeFile.exists()) {
			foundSequence = readLines(expectedListRangeFile);
		}
	}

	
	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println(msg);
	}

	@Test
	public void runSpecialBuildRangeTests() {
		process( new File( output, "build.xml"), "listRange", false, false);
		
		Validator validator = new Validator();
		
		List<String> expectedSequence = new ArrayList<>();		
		expectedSequence.add( "com.braintribe.devrock.test:y#1.0.1");
		expectedSequence.add( "com.braintribe.devrock.test:x#1.0.1");		
		expectedSequence.add( "com.braintribe.devrock.test:a#1.0.1");		
		expectedSequence.add( "com.braintribe.devrock.test:t#1.0.1");
		validator.validateSequence("list-range", foundSequence, expectedSequence);		
		validator.assertResults();
	}
	
	@Test
	public void runSecondSpecialBuildRangeTests() {
		process( new File( output, "build.2.xml"), "listRange", false, false);
		
		Validator validator = new Validator();
		
		List<String> expectedSequence = new ArrayList<>();		
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
