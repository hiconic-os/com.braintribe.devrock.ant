// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.junitextensions;

import static com.braintribe.testing.junit.assertions.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.build.ant.junitextensions.ExtendedXmlJUnitResultFormatter.OutputFormat;
import com.braintribe.build.ant.junitextensions.ExtendedXmlJUnitResultFormatter.OutputStream;
import com.braintribe.utils.DOMTools;
import com.braintribe.utils.FileTools;

/**
 * Provides tests for {@link ExtendedXmlJUnitResultFormatter}.
 *
 * @author michael.lafite
 */
public class ExtendedXmlJUnitResultFormatterTest {

	private static final String DOCUMENT_FILEPATH_OutputPerTestMethodTest = "res/com.braintribe.test.unittesting.testedartifact.output.OutputPerTestMethodTest.xml";
	private static final String DOCUMENT_FILEPATH_OutputFromAllStagesTest = "res/com.braintribe.test.unittesting.testedartifact.output.OutputFromAllStagesTest.xml";

	private static final String DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_input = "res/com.braintribe.test.unittesting.testedartifact.output.OutputFromAllStagesTest_applyFormat_input.xml";
	private static final String DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_ant_expectedOutput  = "res/com.braintribe.test.unittesting.testedartifact.output.OutputFromAllStagesTest_applyFormat_ant_expectedOutput.xml";
	private static final String DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_standard_expectedOutput  = "res/com.braintribe.test.unittesting.testedartifact.output.OutputFromAllStagesTest_applyFormat_standard_expectedOutput.xml";
	
	private static final String DOCUMENT_FILEPATH_FilterPropertiesTest = "res/FilterPropertiesTest.xml";

	@Test
	public void testMoveOutputsToTestCases() {
		Document document = readDocumentFromFile(DOCUMENT_FILEPATH_OutputPerTestMethodTest);
		Element rootElement = document.getDocumentElement();

		ExtendedXmlJUnitResultFormatter.moveOutputsToTestCases(rootElement);

		String formattedResult = DOMTools.toFormattedString(rootElement);

		System.out.println(formattedResult);

		// assert that there is a separate cdata section for test 3
		assertThat(formattedResult).contains("<![CDATA[Output from test method 'test3' written to System.out.");

	}

	@Test
	public void testMoveOutputsToTestCasesButKeepBeforeAndAfterClassLogging() {
		Document document = readDocumentFromFile(DOCUMENT_FILEPATH_OutputFromAllStagesTest);
		Element rootElement = document.getDocumentElement();

		ExtendedXmlJUnitResultFormatter.moveOutputsToTestCases(rootElement);

		String formattedResult = DOMTools.toFormattedString(rootElement);

		System.out.println(formattedResult);

		// Before/After logging is part of test case log
		assertThat(formattedResult).contains(
				"<system-out><![CDATA[_beforeInSuperType2__beforeInSuperType1__before2__before1__test1__after1__after2__afterInSuperType1__afterInSuperType2_]]></system-out>");
		assertThat(formattedResult).contains(
				"<system-out><![CDATA[_beforeInSuperType2__beforeInSuperType1__before2__before1__test2__after1__after2__afterInSuperType1__afterInSuperType2_]]></system-out>");
		// BeforeClass/AFterClass logging is still in general test suite log
		assertThat(formattedResult).contains(
				"<system-out><![CDATA[_beforeClassInSuperType2__beforeClassInSuperType1__beforeClass2__beforeClass1_"+ExtendedXmlJUnitResultFormatter.OUTPUTSTREAM_MARKER_TESTS_OUTPUT_MOVED+"_afterClass1__afterClass2__afterClassInSuperType1__afterClassInSuperType2_]]></system-out>");
	}

	@Test
	public void testFilterProperties() {
		Document document = readDocumentFromFile(DOCUMENT_FILEPATH_FilterPropertiesTest);
		Element rootElement = document.getDocumentElement();

		{
			String formattedResult = DOMTools.toFormattedString(rootElement);
			assertThat(formattedResult).contains("unittests.artifact.fullyQualifiedName");
			assertThat(formattedResult).contains("propertyToRemove");
		}

		{
			// keep single property
			ExtendedXmlJUnitResultFormatter.filterProperties(rootElement, "unittests.artifact.fullyQualifiedName", "");
			String formattedResult = DOMTools.toFormattedString(rootElement);
			// System.out.println(formattedResult);
			assertThat(formattedResult).contains("unittests.artifact.fullyQualifiedName");
			assertThat(formattedResult).doesNotContain("propertyToRemove");
		}

		{
			// remove all properties
			ExtendedXmlJUnitResultFormatter.filterProperties(rootElement, "", "");
			String formattedResult = DOMTools.toFormattedString(rootElement);
			assertThat(formattedResult).doesNotContain("unittests.artifact.fullyQualifiedName");
		}

	}

	@Test
	public void testTruncateTestCaseOutput() {
		Document document = readDocumentFromFile(DOCUMENT_FILEPATH_OutputPerTestMethodTest);
		Element rootElement = document.getDocumentElement();

		ExtendedXmlJUnitResultFormatter.moveOutputsToTestCases(rootElement);
		ExtendedXmlJUnitResultFormatter.truncateTestCaseOutput(rootElement, OutputStream.SysOut, 0, 40);
		ExtendedXmlJUnitResultFormatter.truncateTestCaseOutput(rootElement, OutputStream.SysErr, 40, 10000);

		String formattedResult = DOMTools.toFormattedString(rootElement);
		System.out.println(formattedResult);

		// sysout + success --> removed completely
		assertThat(formattedResult).doesNotContain("<system-out><![CDATA[Output from test method 'test1' written");
		// syserr + success --> truncated
		assertThat(formattedResult).contains("<system-err><![CDATA[Output from test method 'test1' written ... (78 characters truncated)");
		// sysout + failure --> truncated
		assertThat(formattedResult).contains("<system-out><![CDATA[Output from test method 'test8' written ... (78 characters truncated)");
		// syserr + failure --> not truncated
		assertThat(formattedResult).contains("<system-err><![CDATA[Output from test method 'test8' written to System.err.");
	}

	@Test
	public void testApplyFormat_ant() {
		Document document = readDocumentFromFile(DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_input);
		Element rootElement = document.getDocumentElement();

		ExtendedXmlJUnitResultFormatter.applyOutputFormat(rootElement, OutputFormat.ant);

		String formattedResult = DOMTools.toFormattedString(rootElement);
		System.out.println(formattedResult);

		String expectedResult = readDocumentFromFileAsFormattedString(DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_ant_expectedOutput);
		assertThat(formattedResult).isEqualToWithVerboseErrorMessageAndLogging(expectedResult);
	}
	
	@Test
	public void testApplyFormat_standard() {
		Document document = readDocumentFromFile(DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_input);
		Element rootElement = document.getDocumentElement();

		ExtendedXmlJUnitResultFormatter.applyOutputFormat(rootElement, OutputFormat.standard);

		String formattedResult = DOMTools.toFormattedString(rootElement);
		System.out.println(formattedResult);

		String expectedResult = readDocumentFromFileAsFormattedString(DOCUMENT_FILEPATH_OutputFromAllStagesTest_applyFormat_standard_expectedOutput);
		assertThat(formattedResult).isEqualToWithVerboseErrorMessageAndLogging(expectedResult);
	}
	
	private static Document readDocumentFromFile(String filePath) {
		File file = new File(filePath);
		String fileContent = FileTools.readStringFromFile(file);
		Document document = DOMTools.parse(fileContent);
		return document;
	}
	
	private static String readDocumentFromFileAsFormattedString(String filePath) {
		File file = new File(filePath);
		String fileContent = FileTools.readStringFromFile(file);
		Document document = DOMTools.parse(fileContent);
		String content = DOMTools.toFormattedString(document);
		return content;
	}

}
