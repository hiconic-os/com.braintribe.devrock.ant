// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.build.ant.tasks.validation.AbstractPomValidatingTask;
import com.braintribe.build.ant.tasks.validation.PomContentValidatingTask;
import com.braintribe.build.ant.tasks.validation.PomFormatValidatingTask;
import com.braintribe.build.ant.types.AttachedResource;
import com.braintribe.build.ant.utils.FileUtil;
import com.braintribe.build.ant.utils.PartUtils;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.devrock.model.mc.reason.PomValidationReason;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.PartTuple;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.processing.version.VersionProcessingException;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.stream.api.StreamPipe;
import com.braintribe.utils.stream.api.StreamPipes;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;



/**
 * 
 * generic base class for both install and deploy task. <br/>
 * 
 *  to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y <br/>
 * <br/>
 * check out the  getRelevantFiles function:<br/>
 * interprets the {@link AttachedResource} differently as maven does: 
 * if the file property's set, then the file declared is used<br/>
 * if you file the file property empty (or if it's directory), classifier and type is used to build a {@link PartTuple}<br/>
 * if you pass the file as directory, it's used as the base directory for the candidates. 
 * 
 * @author pit
 *
 */
public abstract class BasicInstallOrDeployTask extends AbstractTransferTask {

	private static Logger log = Logger.getLogger(BasicInstallOrDeployTask.class);

	private List<AttachedResource> attachedResources;
	private File file;

	private boolean validatePom = true;
	
	public void setFile(File file) {
		this.file = file;
	} 
	public File getFile() {
		return file;
	}
	
	public void setValidatePom(boolean validatePom) {
		this.validatePom = validatePom;
	}
	
	public void addAttach( AttachedResource attachedResource) {
		if (attachedResources == null)
			attachedResources = newList();

		attachedResources.add( attachedResource);
	}
	
	protected void addPart(Artifact artifact, File file, PartIdentification partIdentification) {
		FileResource fileResource = FileResource.T.create();
		fileResource.setPath(file.getAbsolutePath());
		addPart(artifact, fileResource, partIdentification);
	}
	
	protected void addPart(Artifact artifact, Resource resource, PartIdentification partIdentification) {
		Part part = Part.T.create();
		part.setClassifier(partIdentification.getClassifier());
		part.setType(partIdentification.getType());
		part.setResource(resource);
		artifact.getParts().put(partIdentification.asString(), part);
	}
	
	@Override
	protected void addParts(Artifact artifact) throws BuildException {
		File pomFile = pom.getFile();
		// validate pom here?
		if (validatePom && pomFile.exists()) {
			validatePom(pomFile, "syntactic", new PomFormatValidatingTask());
			validatePom(pomFile, "semantic", new PomContentValidatingTask());
		}

		addPart(artifact, normalizePom(pomFile), PartIdentifications.pom);
		addOtherParts(artifact);
	}

	private void validatePom(File pomFile, String kind, AbstractPomValidatingTask task) {
		if (kind.equals("semantic"))
			task.setProject(getProject());
		task.setPomFile(pomFile);
		PomValidationReason validationIssue = task.runValidation();
		if (validationIssue == null)
			return;

		String msg = kind + " validation of  [" + pomFile.getAbsolutePath() + "] failed : " + validationIssue.stringify();
		log.error(msg);
		throw new BuildException(msg);
	}

	// 
	protected void addOtherParts(Artifact artifact) throws BuildException {
		if (file != null && !file.isDirectory())
			FileUtil.addPartFromCanonizedOrOtherFile(this, artifact, file, true);
		
		// add attachments..
		if (attachedResources != null) {
			for (AttachedResource attachedResource : attachedResources) {
				File attachedFile = attachedResource.getFile();
				// if we have a file, we use that one 
				if (attachedFile != null) {
					if (isExistingFile(attachedFile)) {
						// if type's given, the name is actually derived from the artifact and the type 
						if (attachedResource.getType() != null)
							addPart(artifact, attachedFile, PartIdentification.parse(attachedResource.getType()));
						else
							FileUtil.addPartFromCanonizedOrOtherFile(this, artifact, attachedFile, true);

						continue;
					}

					if (attachedResource.getSkipIfNoFile())
						continue;

					log.warn("attached resource [" + attachedFile.getAbsolutePath() + "] doesn't exist");
				}
				// 
				// we could actually ignore the classifier for our purposes
				String classifier = attachedResource.getClassifier();
				String type = attachedResource.getType();
				
				PartIdentification partTuple = null;
				if (classifier != null) {
					partTuple = PartIdentification.create(classifier, type);
				} else {
					if (type == null) {
						String msg="at least 'type' must be set with a non-null value";
						log.error( msg, null);
						throw new BuildException(msg);
					}
					// find out if type's a valid part type as a string 
					try {
						partTuple = PartUtils.fromPartType(type);
					} catch (Exception e) {
						// no match, so use it as simple string
						partTuple = PartIdentification.parse(type);
					}
				}
				
				try {
					StringBuilder nameBuilder = new StringBuilder();
					
					nameBuilder.append(pom.getArtifactId());
					nameBuilder.append("-");
					nameBuilder.append(pom.getVersion());
					String classifer = partTuple.getClassifier();
					if (classifer != null) {
						nameBuilder.append("-");
						nameBuilder.append(classifier);
					}
					String ext = partTuple.getType();
					
					if (ext != null) {
						nameBuilder.append(".");
						nameBuilder.append(ext);
					}
					
					String name = nameBuilder.toString();
					final File partFile;
					if (file != null && file.isDirectory()) {
						partFile = new File(file, name);
					}
					else {
						partFile = new File(name);
					}

					if (partFile.exists()) {
						addPart(artifact, partFile, partTuple);
						log.debug("requesting addition of matching file [" + file.getAbsolutePath() + "]");
					}
					
				} catch (VersionProcessingException e) {
					log.warn("cannot build a valid version from [" + pom.getVersion() + "]");
				} 
			}
		}
	}

	private boolean isExistingFile(File file) {
		return file.exists() && !file.isDirectory();
	}

	/**
	 * as Maven cannot handle variables properly, we must "dumb down" the pom 
	 * @param key - the file that contains the pom 
	 * @return - the modified contents of the pom
	 */
	protected Resource normalizePom(File key) {
		try {
			Document document = DomParser.load().from(key);
			boolean changed = false;
			// it's a solution actually, as the Pom reader has read it 
			CompiledArtifact solution = getPom().getArtifact();
			Element documentElement = document.getDocumentElement();

			// parent reference
			CompiledDependencyIdentification parentDependency = solution.getParent();
			
			if (parentDependency != null) {
				String resolvedVersionRange = parentDependency.getVersion().asString();
				Element parentElement = DomUtils.getElementByPath( documentElement, "parent/version", false);
				if (parentElement != null) {
					parentElement.setTextContent(resolvedVersionRange);
					changed = true;
				}
			}
			Element versionElement = DomUtils.getElementByPath( documentElement, "version", false);
			if (versionElement != null) {
				versionElement.setTextContent(solution.getVersion().asString()); 
				changed = true;
			}
			if (changed) {
				StreamPipe pipe = StreamPipes.fileBackedFactory().newPipe("normalized-pom");
				try (Writer writer = new BufferedWriter(new OutputStreamWriter(pipe.openOutputStream(), "UTF-8"))) {
					XmlUtils.writeXml(document, writer);
					writer.write("\n");
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				return Resource.createTransient(pipe::openInputStream);
			}
			else {
				FileResource fileResource = FileResource.T.create();
				fileResource.setPath(key.getAbsolutePath());
				return fileResource;
			}
		} catch (DomParserException e) {
			String msg = "cannot dumb down [" + key.getAbsolutePath() + "] so that Maven can read it";
			log.error( msg, e);
			throw new BuildException(msg, e);
		}
	}		
	
	
}
