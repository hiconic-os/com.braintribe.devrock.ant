// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.utils.property;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;

/**
 * Utility class that contains features that can be called within the JSON command<br/>
 * @author pit
 *
 */
public class ExpansionTools {
	/**
	 * escapes the string passed into valid JavaScript notation 
	 * @param s - an {@link Object} to translate (a {@link String} in disguise)
	 * @return - the escaped {@link String}
	 */
	public String escape(Object s) {
	   StringWriter writer = new StringWriter();
	   try {
		   StringEscapeUtils.escapeJavaScript(writer, s.toString());
	   } catch (IOException e) {
		   throw new RuntimeException(e);
	   }
	   return writer.toString();
	}
	
	
	/**
	 * turn a {@link FileSet} into a list of file names 
	 * @param fileSet - the {@link FileSet}
	 * @return - ab {@link List} with the file names as {@link String}
	 */
	public List<String> fileSetAsStringList( FileSet fileSet) {
		List<String> result = new ArrayList<String>();
				
		Iterator<Resource> iterator = fileSet.iterator();
		while (iterator.hasNext()) {
			FileResource file = (FileResource) iterator.next();
			result.add( file.getFile().getAbsolutePath());
		}									
		return result;
	}
	
	/**
	 * turns a {@link FileSet} into a list of {@link FileResource}
	 * @param fileSet - the {@link FileSet}
	 * @return - a {@link List} of {@link FileResource}
	 */
	public List<FileResource> fileSetAsFileResourceList( FileSet fileSet) {
		List<FileResource> result = new ArrayList<FileResource>();
				
		Iterator<Resource> iterator = fileSet.iterator();
		while (iterator.hasNext()) {
			FileResource file = (FileResource) iterator.next();
			result.add( file);
		}									
		return result;
	}
		 
}
