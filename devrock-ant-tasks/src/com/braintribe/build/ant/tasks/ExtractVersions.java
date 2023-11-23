// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;

/**
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 * @author neidhart.orlich
 *
 */
public class ExtractVersions extends Task {
	
	private String range;
	private File targetDirectory;
	private String targetFileName;
	
	@Configurable @Required
	public void setRange(String range) {
		this.range = range;
	}
	
	@Configurable @Required
	public void setTargetFileName(String targetFileName) {
		this.targetFileName = targetFileName;
	}
	
	@Configurable @Required
	public void setTargetDirectory(File targetDirectory) {
		this.targetDirectory = targetDirectory;
	}
	
	@Override
	public void execute() throws BuildException {

		// analyze range, ie. split into artifacts
		List<String> artifacts = Arrays.asList(range.split("\\+"));
		McBridge mcBridge = Bridges.getInstance(getProject());
		
		for (String artifactIdentification: artifacts) {
			CompiledArtifactIdentification artifact = CompiledArtifactIdentification.parse(artifactIdentification); 

			File artifactDir = new File(targetDirectory, artifact.getArtifactId());
			File versionFile = new File(artifactDir, targetFileName);
			File pomFile = new File(artifactDir, "pom.xml");
			
			try (FileWriter fileWriter = new FileWriter(versionFile)){

				CompiledArtifactIdentification effectiveCai = mcBridge.readArtifactIdentification(pomFile);
				
				fileWriter.write(effectiveCai.getVersion().asString());
			} catch (IOException e) {
				e.printStackTrace();
				log(e, 1);
			}

		}
	}
}
