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
 * Replaces occurrences of <code>target</code> in <code>string</code> with <code>replacement</code>.
 *
 * @author michael.lafite
 */
public class Replace extends Task {

	private String string;
	@SuppressWarnings("hiding")
	private String target;
	private String replacement;
	private String resultProperty;

	public void setString(String string) {
		this.string = string;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}

	public void setResultProperty(String resultProperty) {
		this.resultProperty = resultProperty;
	}

	@Override
	public void execute() throws BuildException {
		if (string == null) {
			throw new BuildException("Property 'string' not set!");
		}

		if (target == null) {
			throw new BuildException("Property 'target' not set!");
		}

		if (replacement == null) {
			throw new BuildException("Property 'replacement' not set!");
		}

		if (resultProperty == null) {
			throw new BuildException("Property 'resultProperty' not set!");
		}

		String result = StringTools.replaceAllOccurences(string, target, replacement);

		getProject().setProperty(resultProperty, result);
	}
}
