// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.utils;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;

/**
 * @author peter.gazdik
 */
public class DrAntTools {

	public static BuildLogger findLogger(Project project) {
		return (BuildLogger) project.getBuildListeners().stream() //
				.filter(BuildLogger.class::isInstance) //
				.findFirst() //
				.orElse(null);
	}

	/**
	 * Intention is to wrap custom tasks with this so their stack-traces are printed, as ANT wouldn't do it.
	 * <p>
	 * {@link BuildException}s are not printed, code that throws those is expected to print proper output first and then
	 * throw such an exception.
	 * <p>
	 * This is really meant to analyze bugs, not standard error handling.
	 */
	public static void runAndPrintStacktraceIfNonBuildException(Runnable r) {
		try {
			r.run();

		} catch (RuntimeException e) {
			if (!(e instanceof BuildException))
				e.printStackTrace();
			throw e;
		}
	}
}
