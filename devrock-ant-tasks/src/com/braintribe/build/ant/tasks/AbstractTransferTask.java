// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.utils.lcd.StringTools.isEmpty;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.types.AttachedResource;
import com.braintribe.build.ant.utils.ArtifactResolutionUtil;
import com.braintribe.build.ant.utils.ParallelBuildTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.model.artifact.PartTuple;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;



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
public abstract class AbstractTransferTask extends Task {


	protected Pom pom;
	protected VersionedArtifactIdentification versionedArtifactIdentification;
	
	public void addPom( Pom pom) {
		this.pom = pom;
	}
	
	public Pom getPom() {
		return pom;
	}
	
	@Configurable
	public void setArtifact(String artifact) {
		versionedArtifactIdentification = VersionedArtifactIdentification.parse(artifact);
	}

	
	protected VersionedArtifactIdentification getVersionedArtifactIdentification() {
		if (versionedArtifactIdentification != null)
			return versionedArtifactIdentification;
		
		if (pom == null) {
			throw new BuildException("no artifact coordinates passed. Either the 'artifact' attribute or the 'pom' attribute/poperty must be set");
		}
		CompiledArtifact artifact = pom.getArtifact();
		
		return VersionedArtifactIdentification.create(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion().asString());
	}
	
	
	protected String deriveNameFromType(String type) {
		PartIdentification tuple = PartIdentification.parse(type);

		if (isEmpty(tuple.getClassifier()))
			return pom.getArtifactId() + "-" + pom.getVersion() + "." + tuple.getType();
		else
			return pom.getArtifactId() + "-" + pom.getVersion() + "-" + tuple.getClassifier() + "." + tuple.getType();
	}

	protected abstract Repository getTargetRepository();
	protected abstract void addParts(Artifact installArtifact);
	
	@Override
	public void execute() throws BuildException {
		ParallelBuildTools.runGloballySynchronizedRepoRelatedTask(this::_execute);
	}
	
	private void _execute() throws BuildException {
		new ColorSupport(getProject()).installConsole();
		
		VersionedArtifactIdentification artifact = getVersionedArtifactIdentification();
		 		
		
		// get repository from concrete implementation
		Repository repository = getTargetRepository();
		
		// build artifact with parts
		Artifact transferArtifact = Artifact.T.create();
		transferArtifact.setGroupId(artifact.getGroupId());
		transferArtifact.setArtifactId(artifact.getArtifactId());
		transferArtifact.setVersion(artifact.getVersion());
		
		addParts(transferArtifact);
		
		ArtifactResolution install = Bridges.getInstance(getProject()).deploy(repository, transferArtifact);
		
		Artifact uploadedArtifact = install.getTerminals().get(0);
		
		Comparator<PartIdentification> comparator = comparator(PartIdentification.T, PartIdentification.classifier) // 
			.thenComparing(comparator(PartIdentification.T, PartIdentification.type));
		
		List<Part> sortedParts = uploadedArtifact.getParts().values().stream().sorted(comparator).collect(Collectors.toList());

		ConfigurableConsoleOutputContainer output = ConsoleOutputs.configurableSequence();
		
		output.append(text("transferred parts of "));
		output.append(ArtifactResolutionUtil.outputArtifact(transferArtifact));
		
		for (Part part: sortedParts) {
			String partStr = PartIdentification.asString(part);
			
			Resource resource = part.getResource();
			
			if (resource != null) {
				if (resource instanceof FileResource) {
					FileResource fileResource = (FileResource)resource;
					File pathFile = new File(fileResource.getPath());
					
					String directory = pathFile.getParentFile().getAbsolutePath() + File.separatorChar;
					String fileName = pathFile.getName();
					
					output.append(sequence(
							text("\n  "),
							ConsoleOutputs.brightBlack(directory),
							text(fileName)
					));
				}
				else {
					output.append("\n  " + partStr);
				}
			}
		}
		
		ConsoleOutputs.print(output);
		
		if (install.hasFailed()) {
			throw new BuildException(install.getFailure().stringify());
		}
	}
	
	private static <T extends GenericEntity> Comparator<T> comparator(EntityType<T> type, String propertyName) {
		Property property = type.getProperty(propertyName);
		
		return (e1, e2) -> {
			Comparable<Object> c1 = property.get(e1);
			Comparable<Object> c2 = property.get(e2);
			
			if (c1 == c2)
				return 0;
			
			if (c1 == null)
				return -1;
			
			if (c2 == null)
				return 1;
			
			return c1.compareTo(c2);
		};
	}
}
