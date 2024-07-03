// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
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
