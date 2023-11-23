package com.braintribe.build.ant.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.logging.Logger;
import com.braintribe.model.version.FuzzyVersion;
import com.braintribe.model.version.Version;
import com.braintribe.utils.DOMTools;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

/**
 * extracted to be used in the 'COREDR-123 task'
 * 
 * @author pit
 *
 */
public interface RevisionChangeTrait {

	static Logger log = Logger.getLogger(RevisionChangeTrait.class);
	static final String VERSION_EXPRESSION_PREFIX = "  \"version\": \"";
	static final String versionLineRegex = "(\\t| )*\"version\": \"\\d+\\.\\d+\\.\\d+(-.+)?\",";
	
	/**
	 * find the package.json file in the artifact's directory 
	 * @param artifactDir - the artifact's diretory in the local repository 
	 * @return - the {@link File} if found, null otherwise 
	 */	
	default File findJsonPackage(File artifactDir) {
		File packageFile = new File( artifactDir, "package.json");
		if (packageFile.exists()) {
			return packageFile;
		}
		return null;
	}
	default void writeVersionToJsonPackage(File jsonPackageFile, String version, boolean keepBakOfPackageJson) {
		writeVersionToJsonPackage(jsonPackageFile, Version.parse(version), keepBakOfPackageJson);
	}
	
	/**
	 * write the new version (major/minor plus revision). 
	 * Writes to an '.out file, and renames it after successful completion. Only one 'version'-bla expression is allowed,
	 * zero or more will lead to an exception.
	 * @param jsonPackageFile - the {@link File} containing the package.json data  
	 * @param version - the new version to write
	 */
	default void writeVersionToJsonPackage(File jsonPackageFile, Version version, boolean keepBakOfPackageJson) {
		File out = new File( jsonPackageFile + ".out");
		int foundVersions = 0;
		try (
				BufferedReader reader = new BufferedReader( new FileReader(jsonPackageFile));
				BufferedWriter writer = new BufferedWriter(new FileWriter( out));
			) {
			while (reader.ready()) {
				String line = reader.readLine();
				if (line == null)
					break;
				if (line.matches(versionLineRegex)) {
					foundVersions++;
					// write
					line = VERSION_EXPRESSION_PREFIX + version.asString() + "\",";
				
				}
				writer.write( line + System.lineSeparator()); 
			}				
		} catch (IOException e) {
			throw new IllegalStateException("cannot access file [ " + jsonPackageFile.getAbsolutePath() + "]", e);
		}
		if (foundVersions != 1) {
			// delete the erroneous file
			out.delete();
			throw new IllegalStateException("found not the expected number of version expressions: expected [1], found [" + foundVersions + "]");
		}
		
		if (keepBakOfPackageJson) {
			// if a bak-file should be kept -> handle that 
			File bakFile = new File( jsonPackageFile + ".bak");
			if (bakFile.exists()) {
				bakFile.delete();
			}
			jsonPackageFile.renameTo( bakFile);
		}
		else {
			// no bak file, simply delete 
			jsonPackageFile.delete();
		}
		
		// rename the file 
		out.renameTo( jsonPackageFile);	
	}

	/**
	 * writes the revision to a pom file
	 * @param pomFile - 
	 * @param pomAsDocument
	 * @param revisionE
	 * @param revision
	 */
	default void writeVersionToPom(File pomFile, Document pomAsDocument, Version version) {
		// modify revision to target version
		setVersion(pomAsDocument, version);
		XmlUtils.writeXml(pomFile, pomAsDocument);
	}
	
		
	default Maybe<Version> getVersion(String groupId, Document doc) {
		Element documentElement = doc.getDocumentElement();
		Element versionElement = DOMTools.getFirstElement(documentElement, "version");
		
		// check for legacy style pom which has property placeholders in the version 
		if (versionElement == null)
			return Reasons.build(NotFound.T).text("Missing version element in pom.").toMaybe();
		
		String versionAsStr = versionElement.getTextContent();
		
		// detect legacy version element by placeholders presence 
		if (versionAsStr.contains("${")) 
			return readAndReplaceLegacyVersion(groupId, documentElement);
		
		return Maybe.complete(Version.parse(versionAsStr));
	}
	
	default Maybe<Version> readAndReplaceLegacyVersion(String groupId, Element documentElement) {
		Element propertiesE = DOMTools.getFirstElement(documentElement, "properties");
		if (propertiesE == null) {
			return Reasons.build(NotFound.T).text("Missing properties element in pom.").toMaybe();
		}
		
		List<Element> elementsToBeRemoved = new ArrayList<>(3);
		
		String major = null, minor = null, revision = null;
		Iterator<Element> iterator = DomUtils.getElementIterator(propertiesE, ".*");
		while (iterator.hasNext()) {
			Element propertyE = iterator.next();
			String tagName = propertyE.getTagName();
			
			switch (tagName) {
			case "major":
				major = propertyE.getTextContent();
				elementsToBeRemoved.add(propertyE);
				break;
			case "minor":
				minor = propertyE.getTextContent();
				elementsToBeRemoved.add(propertyE);
				break;
			case "revision":
				revision = propertyE.getTextContent();
				elementsToBeRemoved.add(propertyE);
				break;
			case "nextMinor":
				elementsToBeRemoved.add(propertyE);
				break;
			}
		}
		
		if (major == null || minor == null || revision == null || revision.length() == 0) {
			return Reasons.build(NotFound.T).text("Incomplete version properties in pom.").toMaybe();
		}
		
		for (Element e: elementsToBeRemoved)
			removeWithLeadingWhitespace(e);

		Version version = Version.parse(major + "." + minor + "." + revision);
		
		// find and normalize self group version definition
		Element selfGroupVersion = DOMTools.getFirstElement(propertiesE, "V." + groupId);
		
		if (selfGroupVersion != null) {
			selfGroupVersion.setTextContent(FuzzyVersion.from(version).asString());
		}
		
		// find parent reference and use version range from parsed version
		Element parentE = DOMTools.getFirstElement(documentElement, "parent");
		
		if (parentE != null) {
			Element parentVersionE = DOMTools.getFirstElement(parentE, "version");
			
			if (parentVersionE != null) {
				parentVersionE.setTextContent(FuzzyVersion.from(version).asString());
			}
		}
		
		return Maybe.complete(version);
	}
	
	static void removeWithLeadingWhitespace(Element e) {
		
		Node sibling = e.getPreviousSibling();
		while (sibling != null) {
			if (sibling.getNodeType() == Node.TEXT_NODE) {
				Node node = sibling;
				sibling = sibling.getPreviousSibling();
				node.getParentNode().removeChild(node);
			}
			else
				break;
		}
		
		e.getParentNode().removeChild(e);
	}
	
	default void setVersion(Document doc, Version version) {
		Element documentElement = doc.getDocumentElement();
		Element versionElement = DOMTools.getFirstElement(documentElement, "version");
		versionElement.setTextContent(version.asString());
	}


	/**
	 * writes the version to the pom :
	 * if no variables are found in the version tag, it simply writes the version into the tag.
	 * if if it finds at least one variable (checking for '$') it will overwrite the properties
	 * 'major', 'minor' and 'revision'. If variables exist and it didn't find all 3 properties,
	 * it will warn / fail? 
	 * @param pomFile - the pom file to patch
	 * @param version - the Version to write
	 */
	default void writeVersionToPom(File pomFile, Version version) {
		Document pomAsDocument;
		try {
			pomAsDocument = DomParser.load().from(pomFile);
		} catch (DomParserException e) {
			throw new BuildException("cannot load pom [" + pomFile.getAbsolutePath() + "]", e);
		}		
		// a) find the version tag to see whether there a variables
		Element projectE = pomAsDocument.getDocumentElement();
		Element versionE = DomUtils.getElementByPath( projectE, "version", false);
		boolean isVariableDriven = false;
		if (versionE != null) {
			String textContent = versionE.getTextContent();
			if (textContent.contains( "${")) {
				isVariableDriven = true;
			}
		}
		
		// b) if not variables are found, just write the version to the tag's content.
		if (versionE != null && !isVariableDriven) {
			versionE.setTextContent( version.asString());
		}
		
		// c) add the variables to the properties section - anyhow.
		boolean foundMajor=false, foundMinor=false, foundRevision=false;	
		Element propertiesE = DomUtils.getElementByPath(projectE, "properties", false);
		if (propertiesE != null) {
			Iterator<Element> iterator = DomUtils.getElementIterator(propertiesE, null);
			while (iterator.hasNext()) {
				Element propertyE = iterator.next();
				String tag = propertyE.getTagName();
				String value = propertyE.getTextContent();
				switch (tag) {
					case "major":
						foundMajor = true;
						String newMajorValue = Integer.toString( version.getMajor());
						log.debug("major : exchanging old value [" + value + "] with [" + newMajorValue + "]");
						propertyE.setTextContent( newMajorValue);
						break;
					case "minor":
						foundMinor = true;
						String newMinorValue = Integer.toString( version.getMinor());
						log.debug("major : exchanging old value [" + value + "] with [" + newMinorValue + "]");
						propertyE.setTextContent( newMinorValue);
						break;
					case "revision":
						foundRevision = true;
						String qualifier = version.getQualifier();
						String newRevisionValue = Integer.toString( version.getRevision());
						if (qualifier == null) {
								log.debug("revision : exchanging old value [" + value + "] with [" + newRevisionValue + "]");
								propertyE.setTextContent( newRevisionValue);
							}
							else {
								log.debug("revision : exchanging old value [" + value + "] with [" + newRevisionValue + "-" + qualifier + "]");
								propertyE.setTextContent( newRevisionValue + "-" + qualifier);
							}							
						break;
					default:
						break;							
				}
			}				
			
			// only honk if there seems to be an variable			
			if (isVariableDriven && ( !foundMajor || !foundMinor || !foundRevision)) {
				StringBuilder builder = new StringBuilder();
				if (!foundMajor) {
					if (builder.length() > 0) {
						builder.append(",");
					}
					builder.append("major");
				}
				if (!foundMinor) {
					if (builder.length() > 0) {
						builder.append(",");
					}
					builder.append("minor");
				}
				if (!foundRevision) {
					if (builder.length() > 0) {
						builder.append(",");
					}
					builder.append("revision");
				}
																
				throw new BuildException("pom uses property variables, but not all expected properties were found : missing is/are " + builder.toString());
			}
		}
		
		// dump the pom
		XmlUtils.writeXml(pomFile, pomAsDocument);
	}
}
