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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.logging.LoggerInitializer;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;

public class TestUtils {

	/**
	 * @param file - a single file or a directory
	 */
	public static void delete( File file) {
		if (file == null || file.exists() == false)
			return;
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				if (child.isDirectory()) {
					delete( child);
				} 
				child.delete();			
			}
		}		
		file.delete();		
	}
	
	/**
	 * @param output - the directory to ensure it's there and empty
	 */
	public static void ensure(File output) {	
		if (output.exists())
			delete( output);
		output.mkdirs();
	}
	
	/**
	 * copies a file or a directory to another file or directory 
	 * @param source
	 * @param target
	 */
	public static void copy(File source, File target){
		try {
			FileTools.copyFileOrDirectory(source, target);
		} catch (Exception e) {
			throw new IllegalStateException("cannot copy [" + source.getAbsolutePath() + "] to [" + target.getAbsolutePath() + "]", e);
		}
	}
	
	public static String generateHash(File sourceFile, String alg) {
		return generateHash(sourceFile, Collections.singletonList(alg)).get(alg);
	}
	
	public static Map<String, String> generateHash(File sourceFile, List<String> types) {
		Map<String, String> result = new HashMap<>();
		List<MessageDigest> digests = types.stream().map( t -> {
			try {
				return MessageDigest.getInstance( t);
			} catch (NoSuchAlgorithmException e) {
				throw new IllegalArgumentException("no digest found for [" + t + "]");
			}
		}).collect( Collectors.toList());
	
		byte bytes [] = new byte[65536];
		try (FileInputStream in = new FileInputStream( sourceFile)) {
			int size = 0;
			while ((size = in.read(bytes)) != -1) {
				for (MessageDigest digest : digests)  {
					digest.update(bytes, 0, size);
				}				
			}
			for (int i = 0; i < types.size(); i++)  {
				MessageDigest digest = digests.get( i);
				byte [] digested = digest.digest();
				result.put( types.get(i), StringTools.toHex(digested));
			}
		}
		catch (IOException e) {
			throw new UncheckedIOException("can't read [" + sourceFile.getAbsolutePath() + "]", e);
		}
		return result;
	}

	public static void initializeLogging(File path) {
		LoggerInitializer loggerInitializer = new LoggerInitializer();
		try {							
			File file = new File(path + File.separator + "logger.properties");
			if (file.exists()) {
				loggerInitializer.setLoggerConfigUrl( file.toURI().toURL());		
				loggerInitializer.afterPropertiesSet();
			}
		} catch (Exception e) {		
			String msg = "cannot initialize logging";
			System.err.println(msg);
		}
	}
}
