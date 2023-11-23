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
package com.braintribe.devrock.ant.test.common;

import java.io.File;

import com.braintribe.common.lcd.Pair;

/**
 * allows a common root for test, split into <br/>
 * - input : below all input folders  
 * - output : below all output folders
 * <br/>
 *  i.e. "res/input/maven.pom" and "res/output/maven.pom" instead of "res/maven.pom/input", "res/maven.pom/output"
 * @author pit 
 *
 */
public interface HasCommonFilesystemNode {
	
	/**
	 * @param subpath - the sub path to add to the root 
	 * @return - a {@link Pair} of the input and output directories
	 */
	default Pair<File,File> filesystemRoots( String subpath) {
		File res = new File( "res");	
		File inputRoot = new File( res, "input");
		File input = new File( inputRoot, subpath);
		if (!input.exists())
			input.mkdirs();
		
		File outputRoot = new File( res, "output");
		File output = new File( outputRoot, subpath);
		if (!output.exists()) 
			output.mkdirs();
		
		return Pair.of( input, output);
	}
}
