// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.utils.lcd.StringTools;

/**
 * Changes the order of elements of a path given as {@link String}. Every element on the path that matches given selector is moved to the
 * beginning/end (based on "prepend" flag) of the path.
 * 
 * This is useful when it is desired to guarantee a specific order for elements of the path, e.g. when patching some java classes, we want
 * to make sure the patch comes before the original class on the classpath.
 */
public class PermutePath extends Task {

	private String path;
	private String propertyName;
	private String selector;
	private boolean prepend;
	private String pathSeparator = File.pathSeparator;

	/** Path to be permuted (rearranged) */
	public void setPath(String path) {
		this.path = path;
	}
	
	public void setPathSeparator(String pathSeparator) {
		this.pathSeparator = pathSeparator;
	}

	/** Name of the property which contains the resulting value */
	public void setProperty(String propertyName) {
		this.propertyName = propertyName;
	}

	/** Specifies which elements on the path are moved to the beginning/end (depending on the "prepend" flag) */
	public void setSelector(String selector) {
		this.selector = selector;
	}

	/** If set to true, the matching path elements are moved to the beginning. Default is <tt>false</tt>. */
	public void setPrepend(boolean prepend) {
		this.prepend = prepend;
	}

	@Override
	public void execute() throws BuildException {
		log("executing PermutePath " + propertyName + " " + selector);

		String[] cpEntries = path.split(pathSeparator);

		List<String> matching = newList();
		List<String> nonMatching = newList();

		for (String cpEntry: cpEntries) {
			String normalizedPath = cpEntry.replace('/', '.').replace('\\', '.');
			if (normalizedPath.contains(selector)) {
				matching.add(cpEntry);
			} else {
				nonMatching.add(cpEntry);
			}
		}

		List<String> permutedEntries;
		if (prepend) {
			permutedEntries = matching;
			permutedEntries.addAll(nonMatching);

		} else {
			permutedEntries = nonMatching;
			permutedEntries.addAll(matching);
		}

		String result = StringTools.join(pathSeparator, permutedEntries);

		getProject().setProperty(propertyName, result);
	}

}
