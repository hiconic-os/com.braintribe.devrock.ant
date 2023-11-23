// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.console.ConsoleOutputs.println;
import static com.braintribe.devrock.mc.core.commons.ArtifactResolutionUtil.outputArtifact;
import static com.braintribe.utils.lcd.StringTools.isEmpty;

import java.io.File;

import org.apache.tools.ant.DrAnsiColorLogger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.ant.utils.DrAntTools;
import com.braintribe.build.ant.utils.ParallelBuildTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;
import com.braintribe.utils.lcd.LazyInitialization;

/**
 * import tasks to retrieve the import 
 * @author pit
 * 
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 */
public class ImportTask extends org.apache.tools.ant.taskdefs.ImportTask {
	private static final String PROFILE_USECASE = "PROFILE_USECASE";
	private String artifact;
	private final PartIdentification importTuple = PartIdentification.create("import", "xml");
	private String useCase = "DEVROCK";
	private final LazyInitialization ensureCapabilities = new LazyInitialization(this::initializeEnsureCapabilities);
	
	@Configurable @Required
	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}
	
	@Configurable
	public void setUseCase(String useCase) {
		this.useCase = useCase;
	}

	private void initializeEnsureCapabilities() {
		setPropertyIfNotPresent("devrock.ant.feature.artifact-reflection-generation", "true");
		setPropertyIfNotPresent("devrock.ant.feature.model-declaration-generation", "true");
	}                            
	
	private void setPropertyIfNotPresent(String name, String value) {
		if (getProject().getProperty(name) == null)
			getProject().setProperty(name, value);
	}

	@Override
	public void execute() throws BuildException {
		ensureCapabilities.run();
		ParallelBuildTools.runGloballySynchronizedRepoRelatedTask(this::_execute);
	}

	private void _execute() throws BuildException {
		if (isEmpty(artifact))
			throw new BuildException("Artifact must be set");

		new ColorSupport(getProject()).installConsole();
		
		switchToAnsiOutputIfPossible();

		Part importPart = resolveImportLocation(artifact);
		
		FileResource fileResource = (FileResource)importPart.getResource();
		
		setFile( fileResource.getPath());
		
		super.execute();
	}

	private void switchToAnsiOutputIfPossible() {
		Project project = getProject();
		BuildLogger logger = DrAntTools.findLogger(project);
		if (isDefaultLogger(logger) && shouldUseColors()) {
			project.removeBuildListener(logger);

			DrAnsiColorLogger ansiColorLogger = new DrAnsiColorLogger((DefaultLogger) logger);
			project.addBuildListener(ansiColorLogger);
		}
	}

	private boolean isDefaultLogger(BuildLogger logger) {
		return logger != null && logger.getClass() == DefaultLogger.class;
	}

	private Boolean shouldUseColors;

	private boolean shouldUseColors() {
		if (shouldUseColors == null)
			shouldUseColors = findIfShouldUseColors();
		return shouldUseColors;
	}

	private boolean findIfShouldUseColors() {
		String color = getProject().getProperty("colors");
		if (color == null)
			color = System.getenv("BT__ANT_COLORS");
		
		if (color == null)
			return true;

		return color.toLowerCase().equals("true");
	}

	/**
	 * use Malaclypse to resolve the import file  
	 * @param artifact - the dependency of the import file  
	 * @return - import file location 
	 */
	private Part resolveImportLocation(String artifact) throws BuildException {
		McBridge mcBridge = Bridges.getAntInstance(getProject(), useCase);
		
		CompiledDependencyIdentification dependency = CompiledDependencyIdentification.parseAndRangify(artifact, true);
		
		Artifact resolvedArtifact = mcBridge.resolveArtifact(dependency, importTuple);
		
		Part part = resolvedArtifact.getParts().get(importTuple.asString());
		
		Maybe<File> fileMaybe = getFile(part);
		
		ConfigurableConsoleOutputContainer msg = ConsoleOutputs.configurableSequence();
		msg.append(outputArtifact(resolvedArtifact));
		
		if (fileMaybe.isSatisfied()) {
			msg.append(" -> ");
			msg.append(fileMaybe.get().getAbsolutePath());
		}
		
		println(msg);
		
		return part;
	}

	private Maybe<File> getFile(Part part) {
		Resource resource = part.getResource();
		
		if (resource instanceof FileResource) {
			FileResource fileResource = (FileResource)resource;
			return Maybe.complete(new File(fileResource.getPath()));
		}
		
		return Reasons.build(NotFound.T).text("Part resource is not backed on the filesystem").toMaybe();
	}
}
