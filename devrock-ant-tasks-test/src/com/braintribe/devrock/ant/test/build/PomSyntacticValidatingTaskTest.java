package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.utils.IOTools;

public class PomSyntacticValidatingTaskTest extends TaskRunner {
	
	private String reason;

	@Override
	protected String filesystemRoot() {
		return "validate/syntactic";
	}

	@Override
	protected RepoletContent archiveContent() {
		//return archiveInput("deploy.definition.yaml");
		return RepoletContent.T.create();
	}
	
	

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( input, output);		
	}

	@Override
	protected void preProcess() {		
	}

	@Override
	protected void postProcess() {
		File validation = new File( output, "validation.txt");
		if (validation.exists()) {
			try {
				reason = IOTools.slurp(validation, "UTF-8");
			} catch (IOException e) {
				System.err.println("cannot read file [" + validation.getAbsolutePath() + "]");
			}
		}
	}
	
	@Test
	public void testValidPom() {
		boolean success = process( new File( output, "build.xml"), "validate.valid");
		Assert.assertTrue("expected validation to succeed, but it failed", success);
	}	
	@Test
	public void testValidPomWithParent() {
		boolean success = process( new File( output, "build.xml"), "validate.valid.2");
		Assert.assertTrue("expected validation to succeed, but it failed", success);
	}
	
	@Test
	public void testInValidPom() {
		boolean success = process( new File( output, "build.xml"), "validate.invalid", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
		System.out.println("-> " +  reason);
	}
	@Test
	public void testInValidPom2() {
		boolean success = process( new File( output, "build.xml"), "validate.invalid.2", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}
	
	@Test
	public void testInValidPomWithParent() {
		boolean success = process( new File( output, "build.xml"), "validate.invalid.3", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}
	

	@Test
	public void testMalformedPom() {
		boolean success = process( new File( output, "build.xml"), "validate.invalid.malformed", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}
	
	@Test
	public void testIncompleteParentPom() {
		boolean success = process( new File( output, "build.xml"), "validate.invalid.parent", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}
	@Test
	public void testInvalidParentReferencePom() {
		boolean success = process( new File( output, "build.xml"), "validate.invalid.parent.reference", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}



}
