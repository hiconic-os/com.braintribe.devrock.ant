package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.util.HashMap;

import org.junit.Test;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.Validator;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;

/**
 * tests a simple structure for the bt:ensureRange task.
 *  
 * @author pit
 *
 */
public class EnsureRangeTests extends TaskRunner {

	private String ensuredRange;
	private String ignoredRange;

	@Override
	protected String filesystemRoot() {
		return "ensureRange";
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
	
		File ensuredRangeFile = new File( output, "ensuredRange.txt");
		ensuredRange = loadTextFile( ensuredRangeFile);
		
		File ignoreRangeFile = new File( output, "ignoreRange.txt");
		ignoredRange = loadTextFile(ignoreRangeFile);		
	}

	
	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {
		System.out.println(msg);
	}

	@Test
	public void runBuildRangeTests() {
		process( new File( output, "build.xml"), "ensureRange");
		
		Validator validator = new Validator();
		
		String expectedEnsuredRange = "a+b+parent+t+x+y+z";
		String expectedIgnoredRange = "";
		
		validator.validateString( ensuredRange, expectedEnsuredRange);
		validator.validateString( ignoredRange.trim(), expectedIgnoredRange);
		
		validator.assertResults();
	}
	
}
