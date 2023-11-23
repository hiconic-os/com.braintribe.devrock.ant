// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.utils.StringTools;

/**
 * Removes a suffix from a string.
 *
 * @author michael.lafite
 */
public class RemoveSuffix extends Task {

	private String string;
	private String suffix;
	private String resultProperty;

	public void setString(String string) {
		this.string = string;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setResultProperty(String resultProperty) {
		this.resultProperty = resultProperty;
	}

	@Override
	public void execute() throws BuildException {
		if (string == null) {
			throw new BuildException("Property 'string' not set!");
		}

		if (suffix == null) {
			throw new BuildException("Property 'suffix' not set!");
		}

		if (resultProperty == null) {
			throw new BuildException("Property 'resultProperty' not set!");
		}

		if (!string.endsWith(suffix)) {
			throw new BuildException("Can't remove suffix, because the passed string '" + string + "' does not end with '" + suffix + "'!");
		}

		String result = StringTools.removeSuffix(string, suffix);
		
		getProject().setProperty(resultProperty, result);
	}
}
