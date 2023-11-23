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
package com.braintribe.devrock.ant.test.direct.validate;



import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.braintribe.build.ant.tasks.validation.PomFormatValidatingTask;
import com.braintribe.common.lcd.Pair;
import com.braintribe.devrock.ant.test.common.HasCommonFilesystemNode;

public class DirectFormalPomValidationTest implements HasCommonFilesystemNode {

	private File input;
	@SuppressWarnings("unused")
	private File output;

	public DirectFormalPomValidationTest() {
		Pair<File,File> pair = filesystemRoots( "validate/syntactic");
		input = pair.first;
		output = pair.second;

	}
	
	private void test( String file, boolean expectedToFail) {
		File pomFile = new File( input, file);
		PomFormatValidatingTask pvt = new PomFormatValidatingTask();
		pvt.setPomFile( pomFile);	
		try {
			com.braintribe.devrock.model.mc.reason.PomValidationReason pvr = pvt.runValidation();
			if (!expectedToFail) {
				if (pvr != null) {
					Assert.fail("didn't expect failures, yet got [" + pvr.stringify() + "] on [" + pomFile.getAbsolutePath() + "]");
				}
			}
			else {
				if (pvr == null) {
					Assert.fail("expect failures, yet got none on [" + pomFile.getAbsolutePath() + "]");
				}				
			}
			if (pvr != null) {
				System.out.println( pvr.stringify());
			}
		} catch (Exception e) {
			Assert.fail("unexpected exception on file [" + pomFile.getAbsolutePath() + "] : " + e);
		}
	}
	
	@Test
	public void testValidPom() {
		test( "valid.pom.xml", false);		
	}
	@Test
	public void testValidPomWithParent() {
		test( "valid.pom2.xml", false);		
	}
	
	@Test
	public void testInValidPomWithDuplicates() {
		test( "invalid.pom.xml", true);
	}
	@Test
	public void testInValidPomMissingEntries() {
		test( "invalid.pom2.xml", true);
	}
	
	@Test
	public void testInvalidMalformedPom() {
		test( "malformed.pom.xml", true);
	}
	
	@Test
	public void testIncompleteParentReferencePom() {
		test( "incomplete.parent.pom.xml", true);
	}
	
	@Test
	public void testInvalidParentReferencePom() {
		test( "invalid.version.derivation.pom.xml", true);
	}

}
