package com.braintribe.devrock.ant.test.build;

import java.io.File;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.devrock.ant.test.TaskRunner;
import com.braintribe.devrock.ant.test.common.TestUtils;
import com.braintribe.devrock.model.repolet.content.RepoletContent;
import com.braintribe.model.version.Version;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

public class ChangeVersionTaskTest extends TaskRunner {

	@Override
	protected String filesystemRoot() {
		return "changeVersion";
	}

	@Override
	protected RepoletContent archiveContent() {
		return RepoletContent.T.create();
	}
	

	@Override
	protected void additionalTasks() {
		// copy build file 
		TestUtils.copy( new File(input, "build.xml"), new File(output, "build.xml"));
		TestUtils.copy( new File(input, "no.variables.pom.xml"), new File(output, "no.variables.pom.xml"));
		TestUtils.copy( new File(input, "variables.pom.xml"), new File(output, "variables.pom.xml"));
		TestUtils.copy( new File(input, "mixed.variables.pom.xml"), new File(output, "mixed.variables.pom.xml"));
		TestUtils.copy( new File(input, "incomplete.variables.pom.xml"), new File(output, "incomplete.variables.pom.xml"));
	}

	@Override
	protected void preProcess() {		
	}

	@Override
	protected void postProcess() {			
	}
	
	@Test
	public void testWithoutVariables() {
		process( new File( output, "build.xml"), "changeWithoutVariables", false, false);
		// validate : read back and
		Document doc;
		try {
			doc = DomParser.load().from( new File( output, "no.variables.pom.xml"));
		} catch (DomParserException e) {
			Assert.fail("couldn't read patched document because " + e.getMessage());
			return;
		}
		Element versionE = DomUtils.getElementByPath(doc.getDocumentElement(), "version", false);
		Assert.assertTrue("no version tag found in document", versionE != null); 
		
		String versionValue = versionE.getTextContent();
		Version foundVersion;
		try {
			foundVersion = Version.parse(versionValue);
		} catch (Exception e) {
			Assert.fail("couldn't read convert stored version because " + e.getMessage());
			return;
		}
		
		Version expectedVersion = Version.create(1, 0, 2);
		
		Assert.assertTrue("expected [" + expectedVersion.asString() + "] yet found [" + foundVersion.asString() + "]", expectedVersion.compareTo(foundVersion) == 0);
	
	}
	
	@Test
	public void testWithVariables() {
		process( new File( output, "build.xml"), "changeWithVariables", false, false);	
		Document doc;
		try {
			doc = DomParser.load().from( new File( output, "variables.pom.xml"));
		} catch (DomParserException e) {
			Assert.fail("couldn't read patched document because " + e.getMessage());
			return;
		}
		Element documentElement = doc.getDocumentElement();

		// version must still be unchanged		
		Element versionE = DomUtils.getElementByPath(documentElement, "version", false);
		Assert.assertTrue("no version tag found in document", versionE != null); 
		String version = versionE.getTextContent();
		String expected = "${major}.${minor}.${revision}";
		Assert.assertTrue("expected version tag to be [" + expected + "], yet found [" + version + "]", version.equals(expected));
		
		// properties must have been changed
		Element propertiesE = DomUtils.getElementByPath( documentElement, "properties", false);
		Assert.assertTrue("no properties found", propertiesE != null);
		
		Iterator<Element> iterator = DomUtils.getElementIterator(propertiesE, null);
		while (iterator.hasNext()) {
			Element propertyE = iterator.next();
			String value = propertyE.getTextContent();
			switch (propertyE.getTagName()) {
				case "major":
					Assert.assertTrue("major should be [1], yet was found to be [" + value +  "]", value.equals("1"));
					break;
				case "minor":
					Assert.assertTrue("minor should be [0], yet was found to be [" + value +  "]", value.equals("0"));
					break;
				case "revision":
					Assert.assertTrue("revision should be [2], yet was found to be [" + value +  "]", value.equals("2"));
					break;
				default:
					break;
			}
		}				
	}
	
	@Test
	public void testWithMixedVariables() {
		process( new File( output, "build.xml"), "changeWithMixedVariables", false, false);	
		Document doc;
		try {
			doc = DomParser.load().from( new File( output, "mixed.variables.pom.xml"));
		} catch (DomParserException e) {
			Assert.fail("couldn't read patched document because " + e.getMessage());
			return;
		}
		Element documentElement = doc.getDocumentElement();
		
		// version must be changed 
		Element versionE = DomUtils.getElementByPath(doc.getDocumentElement(), "version", false);
		Assert.assertTrue("no version tag found in document", versionE != null); 
		
		String versionValue = versionE.getTextContent();
		Version foundVersion;
		try {
			foundVersion = Version.parse(versionValue);
		} catch (Exception e) {
			Assert.fail("couldn't read convert stored version because " + e.getMessage());
			return;
		}
		
		Version expectedVersion = Version.create(1, 0, 2);
		
		Assert.assertTrue("expected [" + expectedVersion.asString() + "] yet found [" + foundVersion.asString() + "]", expectedVersion.compareTo(foundVersion) == 0);
	
		// properties must be changed as well 	
		Element propertiesE = DomUtils.getElementByPath( documentElement, "properties", false);
		Assert.assertTrue("no properties found", propertiesE != null);
		
		Iterator<Element> iterator = DomUtils.getElementIterator(propertiesE, null);
		while (iterator.hasNext()) {
			Element propertyE = iterator.next();
			String value = propertyE.getTextContent();
			switch (propertyE.getTagName()) {
				case "major":
					Assert.assertTrue("major should be [1], yet was found to be [" + value +  "]", value.equals("1"));
					break;
				case "minor":
					Assert.assertTrue("minor should be [0], yet was found to be [" + value +  "]", value.equals("0"));
					break;
				case "revision":
					Assert.assertTrue("revision should be [2], yet was found to be [" + value +  "]", value.equals("2"));
					break;
				default:
					break;
			}
		}
				
	}

	@Test
	public void testWithIncompleteVariables() {
		process( new File( output, "build.xml"), "changeWithIncompleteVariables", false, true);	
	}
}
