package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.utils.IOTools;

public class PomSemanticValidationTaskTest extends TaskRunner {
	
	private String reason;

	@Override
	protected String filesystemRoot() {
		return "validate/semantic";
	}

	@Override
	protected RepoletContent archiveContent() {
		return archiveInput("pom.content.validation.definition.yaml");		
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
		boolean success = process( new File( output, "build.xml"), "validate.valid", false, false);
		Assert.assertTrue("expected validation to succeed, but it failed", success);
	}	
	@Test
	public void testValidPomWithParent() {
		boolean  success = process( new File( output, "build.xml"), "validate.valid.2", false, false);
		Assert.assertTrue("expected validation to succeed, but it failed", success);
	}	
	
	@Test
	public void testInValidPomMissingDep() {
		boolean  success = process( new File( output, "build.xml"), "validate.invalid.1", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}	
	@Test
	public void testInValidPomMissingParent() {
		boolean  success = process( new File( output, "build.xml"), "validate.invalid.2", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
	}	
	@Test
	public void testInValidPomMissingImports() {
		boolean  success = process( new File( output, "build.xml"), "validate.invalid.3", false, true);
		Assert.assertTrue("expected validation to fail, but it succeeded", !success);
		System.out.println("-> " +  reason);
	}	

}
