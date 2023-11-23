// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.unittests;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.BaseTest;
import org.apache.tools.ant.taskdefs.optional.junit.BatchTest;
import org.apache.tools.ant.taskdefs.optional.junit.Enumerations;
import org.apache.tools.ant.taskdefs.optional.junit.FormatterElement;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTaskMirror.JUnitTestRunnerMirror;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import com.braintribe.utils.ReflectionTools;
import com.braintribe.utils.ThrowableTools;

/**
 * Extension of {@link JUnitTask} which can handle {@link BtBatchTest}s, which are similar to regular
 * {@link BatchTest}s, but can also specify which methods to run for individual test classes.
 * 
 * @author peter.gazdik
 */
public class BtJUnitTask extends JUnitTask {

	private final Vector<BtBatchTest> btBatchTests = new Vector<>();

	private boolean haltOnError = false;
	private boolean haltOnFail = false;
	private boolean filterTrace = true;
	private boolean fork = false;
	private String failureProperty;
	private String errorProperty;

	public BtJUnitTask() throws Exception {
	}

	@Override
	public void addFormatter(final FormatterElement fe) {
		super.addFormatter(fe);
	}

	@Override
	public void setFiltertrace(boolean value) {
		super.setFiltertrace(value);
		this.filterTrace = value;
	}

	@Override
	public void setHaltonerror(boolean value) {
		super.setHaltonerror(value);
		this.haltOnError = value;
	}

	@Override
	public void setErrorProperty(String propertyName) {
		super.setErrorProperty(propertyName);
		this.errorProperty = propertyName;
	}

	@Override
	public void setHaltonfailure(boolean value) {
		super.setHaltonfailure(value);
		this.haltOnFail = value;
	}

	@Override
	public void setFailureProperty(String propertyName) {
		super.setFailureProperty(propertyName);
		this.failureProperty = propertyName;
	}

	@Override
	public void setFork(boolean value) {
		super.setFork(value);
		this.fork = value;
	}

	public BtBatchTest createBtBatchTest() {
		BtBatchTest test = new BtBatchTest();
		btBatchTests.addElement(test);
		preConfigure(test);
		return test;
	}

	private void preConfigure(BaseTest test) {
		test.setFiltertrace(filterTrace);
		test.setHaltonerror(haltOnError);
		if (errorProperty != null) {
			test.setErrorProperty(errorProperty);
		}
		test.setHaltonfailure(haltOnFail);
		if (failureProperty != null) {
			test.setFailureProperty(failureProperty);
		}
		test.setFork(fork);
	}

	@Override
	public void execute() throws BuildException {
		try {
			if (getIndividualTests().hasMoreElements())
				super.execute();
		} catch (BuildException e) {

			boolean propagateException = false;
			String moreHelpfulMessage;

			if (e.getMessage() != null && e.getMessage()
					.matches("Using loader AntClassLoader.+ on class .+\\: java.lang.NoClassDefFoundError\\: junit/framework/TestListener")) {
				// test framework crashed!
				// we can't do much about it, except provide some information that points to the real issue

				String stackTraceString = ThrowableTools.getStackTraceString(e);

				if (stackTraceString.contains("at org.apache.tools.ant.taskdefs.optional.junit.JUnitTask.logVmCrash")) {
					// do not replace this with ExtendedXmlJUnitResultFormatter.class.getName()! (since class can't be
					// loaded)
					String nameOfCustomFormatterClass = "com.braintribe.build.ant.junitextensions.ExtendedXmlJUnitResultFormatter";
					if (e.getMessage().contains(nameOfCustomFormatterClass)) {
						moreHelpfulMessage = "The JVM crashed while running JUnit tests. One known issue for that is that one of the tests closed System.out or System.err stream."
								+ " Please make sure that does not happen in your tests! If that doesn't fix the problem, please report it.";
					} else {
						moreHelpfulMessage = "The JVM crashed while running JUnit tests. Some class which implements junit.framework.TestListener couldn't be loaded! See exception cause below. Note though that the printed classpath may look (and actually be) correct."
								+ " Ant internally uses a special split classloader with special rules. Please report this including the stacktrace below!";
						propagateException = true;
					}

				} else if (stackTraceString.contains("at org.apache.tools.ant.taskdefs.optional.junit.JUnitTask.logTimeout(")) {
					Integer timeout = (Integer) ReflectionTools.getFieldValue("timeout", this);
					if (timeout != null) {
						moreHelpfulMessage = "Reached timeout while running JUnit tests. Timeout is set in Ant when running the tests. Current setting is "
								+ timeout + "ms. If your tests shouldn't take that long to execute, please check and fix your tests."
								+ " Otherwise please mark your tests with Slow/VerySlow category to exclude them from normal CI runs.";
					} else {
						// should be unreachable
						moreHelpfulMessage = "Reached timeout while running JUnit tests, although no timeout was set. This is not expected and there is a bug in the testing framework. Please report this.";
						propagateException = true;
					}
				} else {
					// should be unreachable
					moreHelpfulMessage = "Unexpected error while running unit tests Some class which implements junit.framework.TestListener couldn't be loaded! See exception cause below. Please report this including the stacktrace below!";
					propagateException = true;
				}

			} else if(e.getMessage() != null && e.getMessage().matches(".+ failed") && (haltOnFail || haltOnError)) {
				// Test com.braintribe.tribefire.cartridge.simple.modelexpert.data.PersonExpertTest failed
				// at org.apache.tools.ant.taskdefs.optional.junit.JUnitTask.actOnTestResult(JUnitTask.java:2240)
				moreHelpfulMessage = "A JUnit test failed and junit task was configured to fail fast!\nOriginal exception message: " + e.getMessage();
				propagateException = true;
			} else {
				moreHelpfulMessage = "Unexpected problem while running JUnit tests. Please report this including the stacktrace below!";
				propagateException = true;
			}
			e.printStackTrace();
			throw new BuildException(moreHelpfulMessage, propagateException ? e : null);
		}
	}

	@Override
	@SuppressWarnings("rawtypes")
	protected Enumeration getIndividualTests() {
		final int count = btBatchTests.size();
		final Enumeration[] enums = new Enumeration[count + 1];
		for (int i = 0; i < count; i++) {
			BtBatchTest batchtest = btBatchTests.elementAt(i);
			enums[i] = batchtest.elements();
		}
		enums[enums.length - 1] = super.getIndividualTests();
		return Enumerations.fromCompound(enums);
	}

	/**
	 * To make implementation for our case (with a single {@link BtBatchTest}) simpler, we always write this
	 * "IGNORETHIS.xml" file as the output. IGNORETHIS is the value of {@link JUnitTestRunnerMirror#IGNORED_FILE_NAME}.
	 * <p>
	 * Why? First, a property file is written which contains the entries about each test, including the configured
	 * out-file. Also, the out-file is written out as a command line argument (when running multiple tests in a single
	 * new JVM). This argument is then parsed in the new JVM to extract the extension only. And this parsing simply
	 * takes everything after the last "IGNORETHIS", so if we would have written anything other than
	 * IGNORETHIS.extension here, the extension would end up being null, and it would literally append the string "null"
	 * to the report file names. This however is the case iff we are running more than a single test. In case of a
	 * single test, the out-file is taken from the command line directly.
	 */
	@Override
	protected File getOutput(final FormatterElement fe, final JUnitTest test) {
		if (hasExactlyOneTest())
			return super.getOutput(fe, test);

		String base = JUnitTaskMirror.JUnitTestRunnerMirror.IGNORED_FILE_NAME;
		final File destFile = new File(test.getTodir(), base + fe.getExtension());
		final String absFilename = destFile.getAbsolutePath();
		return getProject().resolveFile(absFilename);
	}

	private boolean hasExactlyOneTest() {
		Enumeration<?> tests = getIndividualTests();
		if (!tests.hasMoreElements())
			return false;
		tests.nextElement(); // first element
		return !tests.hasMoreElements();
	}

}
