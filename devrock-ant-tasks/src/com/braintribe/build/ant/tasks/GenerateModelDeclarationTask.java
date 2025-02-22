package com.braintribe.build.ant.tasks;

import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.sequence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.model.ModelDeclarations;
import com.braintribe.build.model.UpdateModelDeclaration;
import com.braintribe.common.artifact.ArtifactReflection;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.core.commons.McReasonOutput;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.artifact.compiled.CompiledArtifact;


public class GenerateModelDeclarationTask extends Task {

	private Pom pom;
	private File buildDir;
	private String classpath;
	
	/**
	 * The ant task will generate {@link ArtifactReflection} based on the data stored in the pom.xml ({@link Pom}).
	 * The `compiled` version of the artifact {@link CompiledArtifact} is used. 
	 * 
	 * @param pom The {@link Pom} ant task
	 */
	public void addPom(Pom pom) {
		this.pom = pom;
	}
	
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}
	
	/**
	 * Set the output directory base for this ant task. 
	 */
	public void setBuildDir(File buildDir) {
		this.buildDir = buildDir;
	}
	
	@Override
	public void execute() throws BuildException {
		CompiledArtifact artifact = pom.getArtifact();
		String targetFolder = buildDir.getAbsolutePath();
		
		List<String> buildFolders = new ArrayList<>();
		buildFolders.add(targetFolder);
		
		UpdateModelDeclaration updateModelDeclaration = UpdateModelDeclaration.T.create();
		updateModelDeclaration.setBuildFolders(buildFolders);
		updateModelDeclaration.setTargetFolder(targetFolder);
		updateModelDeclaration.setPomFile(pom.getFile().getAbsolutePath());
		updateModelDeclaration.setClasspath(classpath);
		
		Reason reason = ModelDeclarations.buildModelDeclaration(updateModelDeclaration, f -> Maybe.complete(artifact));
		
		if (reason != null) {
			
			ConsoleOutputs.println(sequence(
					brightRed("Error:\n"),
					new McReasonOutput().output(reason)
			));
			
			throw new BuildException("Error while generating model declaration: " + reason.getText() + "\nSee details above!");
		}
		
		log("Generated model declaration");
	}
}
