// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository.utils;

public class NameUtils {
	/**
	 * extracts the local path of the artifact 
	 */
	public static String getArtifactSvnPath( String group, String name, String version) {			
		String dir = group.replaceAll( "\\.", "/");
		
		String path = dir + "/" + name + "/" + version;
		return path;
	}
}
