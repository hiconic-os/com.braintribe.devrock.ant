// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript;



import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Tests for {@link GenerateNpmPackageTask}
 *  
 * @author peter.gazdik
 */
public class GenerateNpmPackageTaskTest {

	
	@Test
	public void testEnsureGitRepoUrlIsOfHttpKind() throws Exception {
		assertGitUrlIsOfHttpKind("git@github.com:hiconic-os/ssh-url.git", "https://github.com/hiconic-os/ssh-url");
		assertGitUrlIsOfHttpKind("https://github.com/hiconic-os/http-url", "https://github.com/hiconic-os/http-url");
	}

	private void assertGitUrlIsOfHttpKind(String original, String expected) {
		String actual =  GenerateNpmPackageTask.ensureGitRepoUrlIsOfHttpKind(original);

		
		assertThat(actual).isEqualTo(expected);
	}
	
	
}
