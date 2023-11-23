package com.braintribe.build.ant.tasks;

import java.io.File;
import java.util.Iterator;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.devrock.artifact.ArtifactReflectionGenerator;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

/**
 * Ant task to generate an automatic {@link ArtifactReflection} as part of an ant build. 
 * During the ant-build the {@link CompiledArtifact} derived from the {@link Pom} 
 * is used to provide all the needed data. The output 
 * is written into the <b>buildDir</b>, which is a mandatory parameter of the task.
 * 
 * @author Dirk Scheffler
 * @author Ralf Ulrich
 *
 * added check so that the task doesn't do indiscriminately all artifacts, as there are currently artifacts
 * of older branches that cannot handle the output of this task.
 * Hence - even if it hurts - the task now scans the .project file to see whether the ARB is actually
 * anchored there. A property in the pom would've been better (most) probably, but that would have required
 * touching all the artifacts intended to be processed by the ARB again
 * @author pit  
 */
public class GenerateArtifactReflectionTask extends Task {

	private static Logger log = Logger.getLogger(GenerateArtifactReflectionTask.class);
	private static final String ARB_ID = "com.braintribe.devrock.arb.builder.ArtifactReflectionBuilder";
	
	private Pom pom;
	private File buildDir;
	
	
	/**
	 * The ant task will generate {@link ArtifactReflection} based on the data stored in the pom.xml ({@link Pom}).
	 * The `compiled` version of the artifact {@link CompiledArtifact} is used. 
	 * 
	 * @param pom The {@link Pom} ant task
	 */
	public void addPom(Pom pom) {
		this.pom = pom;
	}
	
	
	/**
	 * Set the output directory base for this ant task. 
	 * 
	 * @param {@link File} buildDir
	 */
	public void setBuildDir(File buildDir) {
		this.buildDir = buildDir;
	}
	
	@Override
	public void execute() throws BuildException {
		// only do something if the ARB is anchored in the .project file
		if (!isArtifactElligibleForArtifactReflection()) {
			log.info("project is not elligible for artifact-reflection-building:" + pom.getArtifact().asString());
			return;
		}
		
		ArtifactReflectionGenerator generator = new ArtifactReflectionGenerator();
		Maybe<Void> maybe = generator.generate(pom.getArtifact(), buildDir);
			
		if (maybe.isUnsatisfied()) {
			String err =  "Error while generating artifact reflection: " + maybe.whyUnsatisfied().stringify();
			log.error(err);
			throw new BuildException(err);
		}
	}
	
	/**
	 * scans the .project file (must be sibling to the pom's file), looks for presence of the ARB in the buildSpecs
	 * @return - true if the ARB has been found, false in any other case (also in case of errors)
	 */
	private boolean isArtifactElligibleForArtifactReflection() {
		String pomName = pom.getArtifact().asString();
		File pomFile = pom.getFile();
		if (pomFile == null) {
			log.warn("Pom contains no information about its file, hence project cannot be checked whether it's elligible for artifact-reflection:" + pomName);
			return false;
		}
		File projectFile = new File( pomFile.getParentFile(), ".project");
		if (!projectFile.exists()) {
			log.warn("No .project file found, hence project cannot be checked whether it's elligible for artifact-reflection:" + pomName);
			return false;
		}
		Document document;
		try {
			document = DomParser.load().from(projectFile);
		} catch (DomParserException e) {
			log.warn(".project file cannot be read, hence project cannot be checked whether it's elligible for artifact-reflection:" + pomName, e);
			return false;
		}
		Element buildSpecE = DomUtils.getElementByPath(document.getDocumentElement(), "buildSpec", false);
		if (buildSpecE == null) {
			log.warn(".project contains no 'buildSpec' tag, hence project cannot be checked whether it's elligible for artifact-reflection:" + pomName);
			return false;
		}
		
		Iterator<Element> iterator = DomUtils.getElementIterator(buildSpecE, "buildCommand");
		while (iterator.hasNext()) {
			Element buildCommandE = iterator.next();
			Element builderNameE = DomUtils.getElementByPath(buildCommandE, "name", false);
			if (builderNameE == null) {
				log.warn(".project contains at least one 'buildCommand' tag without name, project cannot be checked whether it's elligible for artifact-reflection:" + pomName);
				continue;
			}
			String builderName = builderNameE.getTextContent();
			if (builderName.equalsIgnoreCase(ARB_ID)) {
				return true;
			}			
		}			
		return false;
	}
}
