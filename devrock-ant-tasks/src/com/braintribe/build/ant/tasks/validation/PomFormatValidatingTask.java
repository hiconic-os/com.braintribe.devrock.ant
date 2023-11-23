package com.braintribe.build.ant.tasks.validation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.cfg.Configurable;
import com.braintribe.devrock.model.mc.reason.InvalidPomFormatReason;
import com.braintribe.devrock.model.mc.reason.MalformedArtifactDescriptor;
import com.braintribe.devrock.model.mc.reason.PomValidationReason;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.version.Version;
import com.braintribe.model.version.VersionExpression;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.dom.DomUtilsException;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

/**
 * simple task that validates the passed pom-file by running it past the maven XSD file.
 * 
 * @author pit
 *
 */
public class PomFormatValidatingTask extends AbstractPomValidatingTask {

	private PomValidationReason pvr;

	private String exposureProperty = "pomValidationResult";

	@Configurable
	public void setExposureProperty(String exposureProperty) {
		this.exposureProperty = exposureProperty;
	}

	@Override
	public void execute() throws BuildException {
		PomValidationReason reason = runValidation();
		if (reason != null) {
			getProject().setProperty(exposureProperty, reason.stringify());
			throw new BuildException("file [" + pomFile.getAbsolutePath() + "] is not valid : " + reason.stringify());
		}

	}

	@Override
	public PomValidationReason runValidation() {
		if (pomFile == null) {
			String msg = "No pom file passed";
			throw new BuildException(msg);
		}
		if (!pomFile.exists()) {
			String msg = "Passed pom file [" + pomFile.getAbsolutePath() + "] doesn't exist";
			throw new BuildException(msg);
		}

		// validate against schema (for some reason, validating while loading wants a DTD?)
		try (InputStream pomStream = new FileInputStream(pomFile);
				InputStream schemaStream = getClass().getResourceAsStream("maven-4.0.0.xsd");
				ByteArrayOutputStream out = new ByteArrayOutputStream();) {

			boolean isValid = DomParser.validate().from(pomStream).schema(schemaStream).makeItSo(out);
			out.flush();

			if (!isValid) {				 
				Reason r = Reasons.build(InvalidPomFormatReason.T) //
						.text(out.toString("UTF-8")) //
						.toReason(); // TODO once on new core, use toReson().buildConsequence(PomValidationReason.T)...

				return Reasons.build(PomValidationReason.T) //
						.text("file [" + pomFile.getAbsolutePath() + "] is not valid on the XSD level") //
						.cause(r) //
						.toReason();
			}

		} catch (FileNotFoundException e) {
			return Reasons.build(PomValidationReason.T)
					.text("cannot load file [" + pomFile.getAbsolutePath() + "] to validate [" + e.getMessage() + "]") //
					.toReason();

		} catch (IOException e) {
			return Reasons.build(PomValidationReason.T)
					.text("cannot open file [" + pomFile.getAbsolutePath() + "] (or internal schema resource) to validate [" + e.getMessage() + "]")
					.toReason();

		} catch (DomParserException e) {
			return Reasons.build(PomValidationReason.T)
					.text("cannot process file [" + pomFile.getAbsolutePath() + "] to validate : [" + e.getMessage() + "]") //
					.toReason();
		}

		// validate on other issues (not caught by the XSD)...
		// in the XSD, <project/> is a valid pom

		// load it again
		Document pomDocument = loadPomDocument();

		// so: it needs artifactid, and if no parent, groupId, version
		try {
			Element projectE = pomDocument.getDocumentElement();
			Element parentE = DomUtils.getElement(projectE, "parent");
			if (parentE == null) {
				// no parent : groupId and version must be declared
				Element groupIdE = DomUtils.getElement(projectE, "groupId");
				if (groupIdE == null) {
					acquirePomValidationReason(pomFile).getReasons()
							.add(Reasons.build(MalformedArtifactDescriptor.T).text("no groupid declared nor derivable from parent").toReason());
				}
				Element versionE = DomUtils.getElement(projectE, "version");
				if (versionE == null) {
					acquirePomValidationReason(pomFile).getReasons()
							.add(Reasons.build(MalformedArtifactDescriptor.T).text("no version declared nor derivable from parent").toReason());
				}
			} else {
				// validate parent reference
				Element parentGroupIdE = DomUtils.getElement(parentE, "groupId");
				Element parentArtifactIdE = DomUtils.getElement(parentE, "artifactId");
				Element parentVersionE = DomUtils.getElement(parentE, "version");

				if (parentGroupIdE == null) {
					acquirePomValidationReason(pomFile).getReasons()
							.add(Reasons.build(MalformedArtifactDescriptor.T).text("parent reference has no groupId").toReason());
				}
				if (parentArtifactIdE == null) {
					acquirePomValidationReason(pomFile).getReasons()
							.add(Reasons.build(MalformedArtifactDescriptor.T).text("parent reference has no artifactId").toReason());
				}
				if (parentVersionE == null) {
					acquirePomValidationReason(pomFile).getReasons()
							.add(Reasons.build(MalformedArtifactDescriptor.T).text("parent reference has no version").toReason());
				} else {
					// has a parent version
					Element versionE = DomUtils.getElement(projectE, "version");
					if (versionE == null) {
						// yet no version -> parent reference may not use a ranged version as the version is not derivable
						VersionExpression parentVersion = VersionExpression.parse(parentVersionE.getTextContent());

						if (parentVersion instanceof Version == false) {
							acquirePomValidationReason(pomFile).getReasons().add(Reasons.build(MalformedArtifactDescriptor.T)
									.text("parent reference has a ranged version, so the pom must have its proper version declared").toReason());
						}
					}
				}
			}
			// with or without parent : artifactId must be declared
			Element artifactIdE = DomUtils.getElement(projectE, "artifactId");
			if (artifactIdE == null) {
				acquirePomValidationReason(pomFile).getReasons()
						.add(Reasons.build(MalformedArtifactDescriptor.T).text("no artifactid declared").toReason());
			}

		} catch (DomUtilsException e) {
			return Reasons.build(PomValidationReason.T).text("cannot examine pom for high-level issues: [" + e.getMessage() + "]").toReason();
		}

		return pvr;
	}

	private Document loadPomDocument() {
		try {
			return DomParser.load().from(pomFile);
		} catch (DomParserException e1) {
			throw new BuildException("cannot load file [" + pomFile.getAbsolutePath() + "] after it has been successfully validated", e1);
		}
	}

	private PomValidationReason acquirePomValidationReason(File pomFile) {
		if (pvr != null)
			return pvr;
		pvr = Reasons.build(PomValidationReason.T).text("invalid pom [" + pomFile.getAbsolutePath() + "]").toReason();
		return pvr;
	}

}
