// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.junitextensions;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import org.junit.runner.RunWith;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import junit.framework.Test;

/**
 * This {@link JUnitResultFormatter} is an extension of {@link XMLJUnitResultFormatter} (see <a href=
 * "https://svn.apache.org/repos/asf/ant/core/trunk/src/main/org/apache/tools/ant/taskdefs/optional/junit/XMLJUnitResultFormatter.java">sources</a>).
 * It provides the following additional features:
 * <ul>
 * <li>Via system properties {@value #SYSTEMPROPERTY_PROPERTIES_INLCUDE_REGEX} and
 * {@value #SYSTEMPROPERTY_PROPERTIES_INLCUDE_REGEX} one can filter properties in the report. This is much faster than
 * first writing the full report to a file and then removing the properties by applying an XSLT.</li>
 * <li>Each test case has its own system-out/system-err section (unless there was no output). Therefore one clearly sees
 * which output came from which test case. Only test case independent output (from BeforeClass/AfterClass) remains in
 * the general output section.</li>
 * <li>Via system properties {@value #SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_SUCCESS},
 * {@value #SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_FAILURE}, {@value #SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_SUCCESS},
 * {@value #SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_FAILURE}, one can define maximum output length. If set to
 * <code>0</code>, the output element is completely removed from the report.</li>
 * </ul>
 *
 * @author michael.lafite
 */
public class ExtendedXmlJUnitResultFormatter extends XMLJUnitResultFormatter {

	enum OutputStream {
		SysOut,
		SysErr;

		private String getXmlConstant() {
			switch (this) {
				case SysOut:
					return SYSTEM_OUT;
				case SysErr:
					return SYSTEM_ERR;
				// unreachable code
				default:
					throw new RuntimeException("Unknown enum! " + this);
			}
		}
	}

	private enum StartTestOrEndTest {
		startTest,
		endTest;
	}

	private static final String OUTPUTSTREAM_TESTMARKER_PREFIX = "\n*** {{" + ExtendedXmlJUnitResultFormatter.class.getSimpleName() + "}} [ ";
	private static final String OUTPUTSTREAM_TESTMARKER_SUFFIX = " ] ***\n";
	static final String OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED = "{{" + ExtendedXmlJUnitResultFormatter.class.getSimpleName()
			+ "}} [ TESTS OUTPUT MOVED FROM HERE ]";

	/**
	 * The common prefix for system properties that are used to configure this formatter.
	 */
	// Note that we don't just get the class name, because we want the string to be shown in Javadoc.
	private static final String SYSTEMPROPERTY_PREFIX = "com.braintribe.build.ant.junitextensions.ExtendedXmlJUnitResultFormatter.";

	private static final String SYSTEMPROPERTY_PROPERTIES_INLCUDE_REGEX = SYSTEMPROPERTY_PREFIX + "propertiesIncludeRegex";
	private static final String SYSTEMPROPERTY_PROPERTIES_EXLCUDE_REGEX = SYSTEMPROPERTY_PREFIX + "propertiesExcludeRegex";

	private static final String SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_SUCCESS = SYSTEMPROPERTY_PREFIX + "sysoutMaxLengthForSuccess";
	private static final String SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_FAILURE = SYSTEMPROPERTY_PREFIX + "sysoutMaxLengthForFailure";
	private static final String SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_SUCCESS = SYSTEMPROPERTY_PREFIX + "syserrMaxLengthForSuccess";
	private static final String SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_FAILURE = SYSTEMPROPERTY_PREFIX + "syserrMaxLengthForFailure";

	private static final String SYSTEMPROPERTY_OUTPUT_FORMAT = SYSTEMPROPERTY_PREFIX + "outputFormat";

	private static final List<String> supportedProperties = Arrays.asList(new String[] { SYSTEMPROPERTY_PROPERTIES_INLCUDE_REGEX,
			SYSTEMPROPERTY_PROPERTIES_EXLCUDE_REGEX, SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_SUCCESS, SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_FAILURE,
			SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_SUCCESS, SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_FAILURE, SYSTEMPROPERTY_OUTPUT_FORMAT });

	private String propertiesIncludeRegex = "";
	private String propertiesExcludeRegex = "";

	private int sysoutMaxLengthForSuccess = Integer.MAX_VALUE;
	private int sysoutMaxLengthForFailure = Integer.MAX_VALUE;
	private int syserrMaxLengthForSuccess = Integer.MAX_VALUE;
	private int syserrMaxLengthForFailure = Integer.MAX_VALUE;

	/**
	 * Specifies the output format.
	 */
	static enum OutputFormat {
		/** Similar to Ant JUnit result formatter. */
		ant,
		/**
		 * A slightly adapted format (compared to {@link #ant}) where test cases have their own logging output
		 * elements. This is supported by the official XSD and e.g. Jenkins can handle it (and also the old DevQA CI).
		 */
		standard
	}

	private OutputFormat outputFormat = OutputFormat.standard;

	private boolean initialized = false;

	/**
	 * Indicates that a test has been started. This field is <code>true</code> by {@link #startTest(Test) startTest} and
	 * remains <code>true</code> until {@link #endTest(Test) entTest} is called.
	 */
	private boolean testStarted = false;

	/**
	 * The root element.
	 */
	private Element rootElement;

	// for debugging only
	private static boolean loggingEnabled = false;
	private StringBuilder logStringBuilder = new StringBuilder();

	private void initialize() {
		Properties properties = System.getProperties();
		Set<String> supportedPropertiesAsSet = new HashSet<String>(supportedProperties);
		for (Object propertyName : properties.keySet()) {
			if (((String) propertyName).startsWith(SYSTEMPROPERTY_PREFIX) && !supportedPropertiesAsSet.contains(propertyName)) {
				throw new RuntimeException("Unsupported property '" + propertyName + "'! Supported properties are: " + supportedProperties);
			}
		}

		propertiesIncludeRegex = System.getProperty(SYSTEMPROPERTY_PROPERTIES_INLCUDE_REGEX, propertiesIncludeRegex);
		propertiesExcludeRegex = System.getProperty(SYSTEMPROPERTY_PROPERTIES_EXLCUDE_REGEX, propertiesExcludeRegex);
		sysoutMaxLengthForSuccess = Integer
				.parseInt(System.getProperty(SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_SUCCESS, "" + sysoutMaxLengthForSuccess));
		sysoutMaxLengthForFailure = Integer
				.parseInt(System.getProperty(SYSTEMPROPERTY_SYSOUT_MAX_LENGTH_FOR_FAILURE, "" + sysoutMaxLengthForFailure));
		syserrMaxLengthForSuccess = Integer
				.parseInt(System.getProperty(SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_SUCCESS, "" + syserrMaxLengthForSuccess));
		syserrMaxLengthForFailure = Integer
				.parseInt(System.getProperty(SYSTEMPROPERTY_SYSERR_MAX_LENGTH_FOR_FAILURE, "" + syserrMaxLengthForFailure));
		outputFormat = Enum.valueOf(OutputFormat.class, System.getProperty(SYSTEMPROPERTY_OUTPUT_FORMAT, "" + outputFormat));

		initialized = true;
	}

	@Override
	public void startTestSuite(final JUnitTest suite) {
		log(" -- startTestSuite --", true);

		log(" -- startTestSuite - before super.startTestSuite -- ");
		super.startTestSuite(suite);
		log(" -- startTestSuite - after super.startTestSuite -- ");

		if (!initialized) {
			log(" -- startTestSuite - initialize -- ");
			initialize();
		}
		// get access to root element (via reflection)
		rootElement = (Element) getSuperClassFieldValue("rootElement");

		log(" -- startTestSuite - end -- ");
	}

	/**
	 * This method is invoked before each test case. It first calls the super method and then prints a marker to
	 * System.out/err. Therefore one knows when log output for each test case started.
	 */
	@Override
	public void startTest(Test test) {
		log(" -- startTest --", true);

		if (testStarted) {
			// shouldn't happen
			throw new RuntimeException(
					"startTest() has been called although field testStarted is already set to true! Has endTest() not been called??");
		}

		log(" -- startTest - printTestMarkerToOutputStreams --");
		printTestMarkerToOutputStreams(test, StartTestOrEndTest.startTest);

		log(" -- startTest - before super.startTest -- ");
		super.startTest(test);
		log(" -- startTest - after super.startTest -- ");

		testStarted = true;

		log(" -- startTest - end -- ");
	}

	/**
	 * This method is invoked after each test case. In addition to calling the super method this method prints a marker
	 * to System.out/err. Therefore one knows when log output for each test case ended.
	 */
	@Override
	public void endTest(Test test) {
		log(" -- endTest --", true);

		/* Note that endTest will call startTest, if startTest has not been called yet. That's a work-around in the Ant
		 * Junit Formatter for the JUnit bug that startTest will never be called, if already the test setup
		 * (--> @BeforeClass) fails. We Have to call endTest first and then afterwards print our marker. */
		log(" -- endTest - before super.endTest -- ");
		super.endTest(test);
		log(" -- endTest - after super.endTest -- ");

		if (!testStarted) {
			// shouldn't happen
			throw new RuntimeException(
					"endTest() has been called although field testStarted is set to false! endTest() either has been called twice or startTest was never called!");
		}

		log(" -- endTest - printTestMarkerToOutputStreams --");
		printTestMarkerToOutputStreams(test, StartTestOrEndTest.endTest);

		testStarted = false;
	}

	/**
	 * {@link #filterProperties(Element, String, String) Filters} unneeded properties and
	 * {@link #moveOutputsToTestCases(Element) creates} separate output sections for each test case. Afterwards calls
	 * the super method.
	 */
	@Override
	public void endTestSuite(final JUnitTest suite) throws BuildException {
		log(" -- endTestSuite --", true);

		log(" -- endTestSuite - filter properties --");
		filterProperties(rootElement, propertiesIncludeRegex, propertiesExcludeRegex);

		if (!hasRunWith(suite.getName())) {
			/* One can comment out the post-processing methods below to create reports which can be used as input for unit
			 * tests which test the methods of this class. */
			log(" -- endTestSuite - moveOutputsToTestCases --");
			moveOutputsToTestCases(rootElement);
	
			log(" -- endTestSuite - truncateTestCaseOutputs --");
			truncateTestCaseOutput(rootElement, OutputStream.SysOut, sysoutMaxLengthForSuccess, sysoutMaxLengthForFailure);
			truncateTestCaseOutput(rootElement, OutputStream.SysErr, syserrMaxLengthForSuccess, syserrMaxLengthForFailure);
	
			log(" -- endTestSuite - applyOutputFormat --");
			applyOutputFormat(rootElement, outputFormat);
		}

		log(" -- endTestSuite - before super.endTestSuite --");
		super.endTestSuite(suite);
		log(" -- endTestSuite - after super.endTestSuite --");

		log(" -- endTestSuite - end --");

		if (loggingEnabled) {
			System.out.println("\n\nDocument after endTestSuite() ***\n" + toString(rootElement));
			System.out.println("\n\nLogging from " + ExtendedXmlJUnitResultFormatter.class.getSimpleName() + ":\n" + logStringBuilder.toString());
		}
	}

	/**
	 * Prints {@link #getTestMarker(String, StartTestOrEndTest, OutputStream) test markers} to both
	 * {@link OutputStream}s.
	 */
	private static void printTestMarkerToOutputStreams(final Test test, StartTestOrEndTest startTestOrEndTest) throws BuildException {
		String testClassName = JUnitVersionHelper.getTestCaseClassName(test);

		if (hasRunWith(testClassName))
			return;
		
		String testId = getTestId(test);
		System.out.print(getTestMarker(testId, startTestOrEndTest, OutputStream.SysOut));
		System.err.print(getTestMarker(testId, startTestOrEndTest, OutputStream.SysErr));
	}
	
	private static boolean hasRunWith(String testClassName) {
		try {
			// todo: check superclass
			return Class.forName(testClassName).getAnnotationsByType(RunWith.class).length > 0;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns a marker for the passed <code>test</code> (containing its {@link #getTestId(Test) id}.
	 */
	private static String getTestMarker(String testId, StartTestOrEndTest startTestOrEndTest, OutputStream outputStream) throws BuildException {
		String testMarker = OUTPUTSTREAM_TESTMARKER_PREFIX + testId + " " + startTestOrEndTest + " " + outputStream + OUTPUTSTREAM_TESTMARKER_SUFFIX;
		return testMarker;
	}

	/**
	 * Returns the {@link #getTestId(String, String) test id}.
	 */
	private static String getTestId(final Test test) throws BuildException {
		return getTestId(JUnitVersionHelper.getTestCaseClassName(test), JUnitVersionHelper.getTestCaseName(test));
	}

	/**
	 * Returns <code>testCaseClassName/testCaseName</code>, e.g. <code>MathTest/testSum</code>.
	 */
	private static String getTestId(String testCaseClassName, String testCaseName) throws BuildException {
		return testCaseClassName + "/" + testCaseName;
	}

	/**
	 * @see #getTestId(String, String)
	 */
	private static String getTestId(Element testCaseElement) throws BuildException {
		String testCaseClassName = testCaseElement.getAttribute(ATTR_CLASSNAME);
		String testCaseName = testCaseElement.getAttribute(ATTR_NAME);
		return getTestId(testCaseClassName, testCaseName);
	}

	/** See {@link #moveOutputToTestCases(Element, OutputStream)}. */
	static void moveOutputsToTestCases(Element rootElement) {
		moveOutputToTestCases(rootElement, OutputStream.SysOut);
		moveOutputToTestCases(rootElement, OutputStream.SysErr);
	}

	/**
	 * The elements {@value #SYSTEM_OUT} and {@value #SYSTEM_ERR} contain the full output from all test cases. However,
	 * in {@link #startTest(Test)} and {@link #endTest(Test)} we added start/end markers for each test case. This method
	 * iterates through each test case and checks if there is any output. If there is, a separate output element will be
	 * added to the test case element and the respective output will be no remaining (general) output at the end. In
	 * some cases (e.g. BeforeClass/AfterClass logging) there may still be general output though. In this case we keep
	 * it.
	 * <p>
	 * Note that it may happen that there are less markers than test cases. This can happen, if there are multiple
	 * failures in BeforeClass/AfterClass (in which case multiple test case elements will be created). This is just
	 * ignored.
	 */
	private static void moveOutputToTestCases(Element rootElement, OutputStream outputStream) {

		String fullOutput = removeOutputElementAndGetTextOrEmptyString(rootElement, outputStream);

		String remainingOutput = fullOutput;

		NodeList testCaseElements = rootElement.getElementsByTagName(TESTCASE);

		boolean testsOutputMovedMarkerSet = false;

		for (int i = 0; i < testCaseElements.getLength(); i++) {

			Element testCaseElement = (Element) testCaseElements.item(i);

			String testCaseClassName = testCaseElement.getAttribute(ATTR_CLASSNAME);
			String testCaseName = testCaseElement.getAttribute(ATTR_NAME);
			String testId = getTestId(testCaseClassName, testCaseName);
			String startTestMarker = getTestMarker(testId, StartTestOrEndTest.startTest, outputStream);
			String endTestMarker = getTestMarker(testId, StartTestOrEndTest.endTest, outputStream);

			int startTestMarkerIndex = remainingOutput.indexOf(startTestMarker);
			int endTestMarkerIndex = remainingOutput.indexOf(endTestMarker);

			// make sure everything is okay
			{
				if (startTestMarkerIndex >= 0 && (endTestMarkerIndex < 0)) {
					if (remainingOutput.endsWith(startTestMarker)) {
						/* This can happen, if there is a failure in AfterClass method as demonstrated by
						 * com.braintribe.test.unittesting.testedartifact.error.AfterClassFailure (startTest is called,
						 * but endTest isn't!). In this case we just "repair" the remaining output. */
						endTestMarkerIndex = remainingOutput.length();
						remainingOutput = remainingOutput += endTestMarker;

					} else {
						throw new RuntimeException("Found start test marker but no end test marker! (testId=" + testId + ", remaining output='"
								+ remainingOutput + "', full output='" + fullOutput + "')");
					}
				}
				if (startTestMarkerIndex < 0 && (endTestMarkerIndex >= 0)) {
					throw new RuntimeException("Found end test marker but no start test marker! (testId=" + testId + ", remaining output='"
							+ remainingOutput + "', full output='" + fullOutput + "')");
				}
				if (startTestMarkerIndex > endTestMarkerIndex && endTestMarkerIndex >= 0) {
					throw new RuntimeException("Found end test marker before start test marker! (testId=" + testId + ", remaining output='"
							+ remainingOutput + "', full output='" + fullOutput + "')");
				}
			}

			if (startTestMarkerIndex < 0) {
				// no marker for this test; this can happen in case of multiple @BeforeClass/AfterClass failures
				continue;
			}

			String testsOutputMovedMarker = "";
			if (!testsOutputMovedMarkerSet) {
				// dependent on the configured output format, it might be necessary to move test logs back later, thus
				// we set a marker ...
				testsOutputMovedMarker = OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED;
				testsOutputMovedMarkerSet = true;
			}

			String testCaseOutput = remainingOutput.substring(startTestMarkerIndex + startTestMarker.length(), endTestMarkerIndex);
			remainingOutput = remainingOutput.substring(0, startTestMarkerIndex) + testsOutputMovedMarker
					+ remainingOutput.substring(endTestMarkerIndex + endTestMarker.length());

			if (testCaseOutput.length() > 0) {
				addOutputElement(testCaseElement, outputStream, testCaseOutput);
			}
		}

		addOutputElement(rootElement, outputStream, remainingOutput);
	}

	private static Element removeOutputElement(Element parentElement, OutputStream outputStream) {
		Element result = null;
		if (hasElement(parentElement, outputStream.getXmlConstant())) {
			result = getSingleElement(parentElement, outputStream.getXmlConstant());
			removeNode(result);
		}
		return result;
	}

	private static String removeOutputElementAndGetTextOrEmptyString(Element parentElement, OutputStream outputStream) {
		Element outputElement = removeOutputElement(parentElement, outputStream);
		String result = outputElement != null ? outputElement.getTextContent() : "";
		return result;
	}

	private static void replaceInOutputElement(Element parentElement, OutputStream outputStream, String target, String replacement) {
		Element outputElement = removeOutputElement(parentElement, outputStream);
		if (outputElement != null) {
			String output = outputElement.getTextContent();
			output = output.replace(target, replacement);
			addOutputElement(parentElement, outputStream, output);
		}
	}

	private static void addOutputElement(Element parentElement, OutputStream outputStream, String output) {
		Document document = parentElement.getOwnerDocument();
		Element outputElement = document.createElement(outputStream.getXmlConstant());
		parentElement.appendChild(outputElement);
		CDATASection cdataSection = document.createCDATASection(output);
		outputElement.appendChild(cdataSection);
	}

	/**
	 * Checks the length of test case outputs (for the specified <code>outputStream</code> and truncates, if necessary.
	 */
	static void truncateTestCaseOutput(Element rootElement, OutputStream outputStream, int maxLengthForSuccess, int maxLengthForFailure) {

		NodeList testCaseElements = rootElement.getElementsByTagName(TESTCASE);

		for (int i = 0; i < testCaseElements.getLength(); i++) {

			Element testCaseElement = (Element) testCaseElements.item(i);

			if (!hasElement(testCaseElement, outputStream.getXmlConstant())) {
				// nothing to truncate
				continue;
			}

			Element outputElement = getSingleElement(testCaseElement, outputStream.getXmlConstant());

			boolean success = !hasElement(testCaseElement, ERROR) && !hasElement(testCaseElement, FAILURE);

			int maxLength = success ? maxLengthForSuccess : maxLengthForFailure;

			if (maxLength == 0) {
				// since max length is 0, output is apparently not needed at all in this case
				// therefore we also skip the "truncated" messages and just have no output element at all
				removeNode(outputElement);
				continue;
			}

			final String fullOutput = outputElement.getTextContent();

			int charactersToTruncate = fullOutput.length() - maxLength;
			if (charactersToTruncate <= 0) {
				// nothing to truncate, keep output
				continue;
			}

			// truncate output and add message
			String truncatedOutput = fullOutput.substring(0, maxLength);
			truncatedOutput += "... (" + charactersToTruncate + " characters truncated)";

			// replace existing output element
			removeNode(outputElement);
			addOutputElement(testCaseElement, outputStream, truncatedOutput);
		}
	}

	static void applyOutputFormat(Element rootElement, OutputFormat outputFormat) {
		switch (outputFormat) {
			case standard:
				// just remove the marker (i.e. where we removed the test output)
				replaceInOutputElement(rootElement, OutputStream.SysOut, OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED, "");
				replaceInOutputElement(rootElement, OutputStream.SysErr, OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED, "");
				break;
			case ant: {
				/* Collect all test specific output (and remove output elements) and move output to main output element,
				 * BUT with additional information additional so that one sees what has been logged by which test. */

				StringBuilder sysoutOutput = new StringBuilder();
				StringBuilder syserrOutput = new StringBuilder();
				NodeList testCaseElements = rootElement.getElementsByTagName(TESTCASE);
				for (int i = 0; i < testCaseElements.getLength(); i++) {

					Element testCaseElement = (Element) testCaseElements.item(i);

					String testId = getTestId(testCaseElement);

					String testSysoutOutput = removeOutputElementAndGetTextOrEmptyString(testCaseElement, OutputStream.SysOut);
					String testSyserrOutput = removeOutputElementAndGetTextOrEmptyString(testCaseElement, OutputStream.SysErr);

					if (!testSysoutOutput.isEmpty()) {
						sysoutOutput.append("\n--- " + testId + " start ---\n" + testSysoutOutput + "\n--- " + testId + " end ---\n");
					}
					if (!testSyserrOutput.isEmpty()) {
						syserrOutput.append("\n--- " + testId + " start ---\n" + testSyserrOutput + "\n--- " + testId + " end ---\n");
					}
				}

				replaceInOutputElement(rootElement, OutputStream.SysOut, OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED, sysoutOutput.toString());
				replaceInOutputElement(rootElement, OutputStream.SysErr, OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED, syserrOutput.toString());
			}
				break;
			default:
				// should be unreachable
				throw new RuntimeException("Unknown enum " + outputFormat + "!");
		}
	}
	/**
	 * Filters properties based on include/exclude regex.
	 */
	static void filterProperties(Element rootElement, String propertiesIncludeRegex, String propertiesExcludeRegex) {
		Element propertiesElement = getSingleElement(rootElement, PROPERTIES);
		NodeList propertyElements = propertiesElement.getElementsByTagName(PROPERTY);

		for (int i = 0; i < propertyElements.getLength(); i++) {
			Element propertyElement = (Element) propertyElements.item(i);
			String propertyName = propertyElement.getAttribute(ATTR_NAME);
			if (!propertyName.matches(propertiesIncludeRegex) || propertyName.matches(propertiesExcludeRegex)) {
				removeNode(propertyElement);
				i--;
			}
		}
	}

	// ********************** Helpers ********************************

	private static void removeNode(final Node node) {
		final Node parentNode = node.getParentNode();
		if (parentNode != null) {
			parentNode.removeChild(node);
		}
	}

	private static boolean hasElement(Element parent, String name) {
		List<Element> elements = getChildElementsByTagName(parent, name);
		return !elements.isEmpty();
	}

	private static Element getSingleElement(Element parent, String name) {
		List<Element> elements = getChildElementsByTagName(parent, name);
		if (elements.size() != 1) {
			throw new RuntimeException("Expected 1 '" + name + "' element, but found " + elements.size() + "!:\n" + toString(parent));
		}
		return elements.get(0);
	}

	private static List<Element> getChildElementsByTagName(Element parent, String name) {
		NodeList nodeList = parent.getChildNodes();
		List<Element> elements = new ArrayList<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Element && node.getNodeName().equals(name)) {
				elements.add((Element) node);
			}
		}
		return elements;
	}

	private Object getSuperClassFieldValue(String fieldName) {
		Field searchedField = null;
		{
			final Field[] fields = XMLJUnitResultFormatter.class.getDeclaredFields();
			for (final Field field : fields) {
				if (field.getName().equals(fieldName)) {
					searchedField = field;
					break;
				}
			}
		}

		if (searchedField == null) {
			throw new IllegalArgumentException("Coudln't find field '" + fieldName + "'!");
		}
		try {
			searchedField.setAccessible(true);
			Object result = searchedField.get(this);
			return result;
		} catch (final Exception e) {
			throw new RuntimeException("Error while trying to access field '" + fieldName + "'.", e);
		}
	}

	private static String toString(Node node) {
		try {
			java.io.StringWriter writer = new java.io.StringWriter();
			javax.xml.transform.dom.DOMSource src = new javax.xml.transform.dom.DOMSource(node);
			javax.xml.transform.stream.StreamResult res = new javax.xml.transform.stream.StreamResult(writer);
			javax.xml.transform.TransformerFactory factory = javax.xml.transform.TransformerFactory
					.newInstance("com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl", null);
			factory.setAttribute("indent-number", new Integer(2));
			javax.xml.transform.Transformer tr = factory.newTransformer();
			tr.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "ISO-8859-1");
			tr.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
			tr.transform(src, res);
			return writer.toString();
		} catch (javax.xml.transform.TransformerException e) {
			throw new RuntimeException("Error while getting string representation of node " + node + "!", e);
		}
	}

	private void log(Object message) {
		log(message, false);
	}

	/**
	 * Logs to {@link #logStringBuilder}, optionally including the stack trace. Reason to use the string builder is that
	 * System.out/err will be redirected to the report (as if it would have been logged by the unit test). With the
	 * builder, we can log after running the full test suite.
	 */
	private void log(Object message, boolean printStackTrace) {
		if (loggingEnabled) {
			// System.out.println(message);
			logStringBuilder.append(message + "\n");

			if (printStackTrace) {
				final StringBuilder stackTraceBuilder = new StringBuilder();
				final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
				final int indexOfFirstElementToPrint = 2;
				for (int i = indexOfFirstElementToPrint; i < stackTrace.length; i++) {
					stackTraceBuilder.append("\t" + stackTrace[i].toString() + "\n");
				}
				logStringBuilder.append(stackTraceBuilder + "\n");
			}
		}
	}
}
