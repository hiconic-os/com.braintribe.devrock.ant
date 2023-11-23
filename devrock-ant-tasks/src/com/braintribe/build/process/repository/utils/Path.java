// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository.utils;

import java.util.Collections;
import java.util.Stack;
import java.util.regex.Matcher;

@SuppressWarnings("serial")
public class Path extends Stack<String> {
	private String delimiter;
	
	
	public Path(String delimiter) {
		this.delimiter = delimiter; 
	}
	
	public Path(String path, String delimiter) {
		this.delimiter = delimiter;
		Collections.addAll(this, path.split(Matcher.quoteReplacement(delimiter)));
	}
	
	public Path(Path superPath, String path) {
		this.delimiter = superPath.delimiter;
		this.addAll(superPath);
		Collections.addAll(this, path.split(Matcher.quoteReplacement(delimiter)));
	}
	
	@Override
	public synchronized String toString() {
		StringBuilder builder = new StringBuilder();
		for (String part: this) {
			if (builder.length() > 0) builder.append(delimiter);
			builder.append(part);
		}
		
		return builder.toString();
	}
	
	public Path getSubPath(String path) {
		return new Path(this, path);
	}
}
