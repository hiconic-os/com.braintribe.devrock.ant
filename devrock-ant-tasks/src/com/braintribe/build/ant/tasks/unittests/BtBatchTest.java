// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.unittests;

import static java.util.Collections.emptyList;

import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.BaseTest;
import org.apache.tools.ant.taskdefs.optional.junit.BatchTest;
import org.apache.tools.ant.taskdefs.optional.junit.Enumerations;
import org.apache.tools.ant.taskdefs.optional.junit.FormatterElement;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import com.braintribe.utils.lcd.CommonTools;

/**
 * Similar to regular {@link BatchTest}s, but can also specify which methods to run for individual test classes.
 * 
 * @see #setClassesAndMethods(String)
 * 
 * @author peter.gazdik
 */
public class BtBatchTest extends BaseTest {

	private String classesAndMethods;
	private String reportPrefix;
	private int reportMaxLength;

	/**
	 * String specifying classes and their methods to run, using the following format (fully compatible wit the output
	 * of {@link FindJUnitTestsTask}):
	 * 
	 * <pre>
	 * className1:method1,method2,method3^className2:[ALL_METHODS]^className3:methodX,methodY....
	 * </pre>
	 */
	public void setClassesAndMethods(String classesAndMethods) {
		this.classesAndMethods = classesAndMethods;
	}

	public void setReportPrefix(String reportPrefix) {
		this.reportPrefix = reportPrefix;
	}

	public void setReportMaxLength(int reportMaxLength) {
		this.reportMaxLength = reportMaxLength;
	}

	public Enumeration<?> elements() {
		JUnitTest[] tests = createAllJUnitTest();
		return Enumerations.fromArray(tests);
	}

	private JUnitTest[] createAllJUnitTest() {
		List<TestEntry> entries = parseTestEntries();

		return entries.stream() //
				.map(this::toJUnitTest) //
				.toArray(JUnitTest[]::new);
	}

	private List<TestEntry> parseTestEntries() {
		if (classesAndMethods.isEmpty())
			return emptyList();

		String[] classWithMethodsEntries = classesAndMethods.split(Pattern.quote(FindJUnitTestsTask.DEFAULT_TEST_CLASSES_SEPARATOR));
		return Stream.of(classWithMethodsEntries) //
				.map(TestEntry::new) //
				.collect(Collectors.toList());
	}

	private JUnitTest toJUnitTest(TestEntry entry) {
		JUnitTest test = new JUnitTest();
		test.setName(entry.className);
		test.setOutfile(outfile(entry.className));
		if (entry.hasMethodNames())
			test.setMethods(entry.methodsList);

		// The rest of the method is just copied from BatchTest.createJUnitTest
		test.setHaltonerror(this.haltOnError);
		test.setHaltonfailure(this.haltOnFail);
		test.setFiltertrace(this.filtertrace);
		test.setFork(this.fork);
		test.setIf(getIfCondition());
		test.setUnless(getUnlessCondition());
		test.setTodir(this.destDir);
		test.setFailureProperty(failureProperty);
		test.setErrorProperty(errorProperty);
		test.setSkipNonTests(isSkipNonTests());
		Enumeration<?> list = this.formatters.elements();
		while (list.hasMoreElements())
			test.addFormatter((FormatterElement) list.nextElement());

		return test;
	}

	private String outfile(String className) {
		String fileName = reportPrefix + className;

		if (fileName.length() > reportMaxLength) {
			String timestampSuffix = "_" + new Date().getTime();
			if (timestampSuffix.length() > reportMaxLength) // this really shouldn't happen!
				throw new BuildException("cannot create file names for specified maximum file name length " + reportMaxLength + "!");

			fileName = fileName.substring(0, (reportMaxLength - timestampSuffix.length())) + timestampSuffix;
		}

		return fileName;
	}

	private static class TestEntry {
		String className;
		String methodsList;

		public TestEntry(String classWithMethods) {
			String[] values = classWithMethods.split(FindJUnitTestsTask.DEFAULT_CLASS_METHODS_SEPARATOR);

			if (values.length > 2)
				throw new BuildException("Invalid class and methods entry: " + classWithMethods);

			className = values[0];
			if (values.length > 1)
				methodsList = values[1];
		}

		public boolean hasMethodNames() {
			return !CommonTools.isEmpty(methodsList) && !methodsList.equals(FindJUnitTestsTask.DUMMYMETHODNAME_FOR_ALLMETHODS);
		}
	}

	@Override
	public String toString() {
		return "BtBatchTest(" + classesAndMethods + ")";
	}
}
