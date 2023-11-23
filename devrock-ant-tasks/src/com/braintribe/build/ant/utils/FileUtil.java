// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.resource.FileResource;

public class FileUtil {
	public static void addPartFromCanonizedOrOtherFile(Task task, Artifact artifact, File file, boolean allowDenormalizedFile) {
		String normalPrefix = artifact.getArtifactId() + "-" + artifact.getVersion();
		String name = file.getName();
		
		
		Part part = Part.T.create();
		final String key;
		
		if (name.startsWith(normalPrefix)) {
			String remainder = name.substring(normalPrefix.length());
			
			int index = remainder.indexOf('.');
			
			String classifierCandidate;
			String type = null;
			String classifier = null;
			
			if (index != -1) {
				classifierCandidate = remainder.substring(0, index);
				type = remainder.substring(index + 1);
			} 
			else {
				classifierCandidate = remainder;
			}

			if (classifierCandidate.startsWith("-")) {
				classifier = classifierCandidate.substring(1);
			}
			else if (!classifierCandidate.isEmpty()) 
				throw new IllegalStateException("irritating part file name: " + name);
			
			part.setType(type);
			part.setClassifier(classifier);
			
			key = PartIdentification.asString(part);
		}
		else {
			if (allowDenormalizedFile) { 
				key = name;
				part.setType("<escape>");
			}
			else {
				task.log("file parameter must hold a canonized file name like artifactId-version-classifier.type but ["+ file + "] does not comply", Project.MSG_WARN);
				return;
			}
		}
		
		FileResource fileResource = FileResource.T.create();
		fileResource.setPath(file.getAbsolutePath());
		
		part.setResource(fileResource);
		
		artifact.getParts().put(key, part);
	}

	public static File createTmpDirectory() throws BuildException {
		try {
			File file = File.createTempFile("temp-dir", null);
			file.delete();
			file.mkdirs();
			return file;
		} catch (IOException e) {
			throw new BuildException(e);
		}
	}
	
	static public boolean deleteDirectory(File path) {
	    if( path.exists() ) {
	      File[] files = path.listFiles();
	      for(int i=0; i<files.length; i++) {
	         if(files[i].isDirectory()) {
	           deleteDirectory(files[i]);
	         }
	         else {
	           files[i].delete();
	         }
	      }
	    }
	    return( path.delete() );
	  }
	
	/**
	 * cheap file writer : writes a string to a file. 
	 */
	static public boolean writeToFile( File file, String contents) {
		try {
			Writer writer = new BufferedWriter(new FileWriter(file));
			writer.write( contents);
			writer.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
