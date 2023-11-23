// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;

public class FileUtils {
	public static File createTmpDirectory() throws SourceRepositoryAccessException {
		try {
			File file = File.createTempFile("temp-dir", null);
			file.delete();
			file.mkdirs();
			return file;
		} catch (IOException e) {
			throw new SourceRepositoryAccessException(e);
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
	
	 public static void copy(File inputFile, File outputFile) throws SourceRepositoryAccessException {		    

		 	FileReader in = null;
		 	FileWriter out = null;
		    try {
				in = new FileReader(inputFile);
				out = new FileWriter(outputFile);
				
				try {
					int c;

					while ((c = in.read()) != -1)
					  out.write(c);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			} catch (FileNotFoundException e) {
				throw new SourceRepositoryAccessException( "cannot copy files as " + e, e);
			} catch (IOException e) {
				throw new SourceRepositoryAccessException( "cannot copy files as " + e, e);
			}
			
			finally {
				try {
					if (in != null)
						in.close();
					if (out != null)
						out.close();
				} catch (IOException e) {
					throw new SourceRepositoryAccessException( "cannot close files as " + e, e);
				}
			}
		  }
}
