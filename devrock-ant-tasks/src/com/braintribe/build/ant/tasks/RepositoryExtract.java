// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.resource.FileResource;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.paths.PathList;
import com.braintribe.wire.api.util.Sets;

public class RepositoryExtract extends Task {
	private File pomFile;
	private String fileSetId;
	private File dependenciesLogFile;
	private File globalExclusionsFile;
	
	public void setGlobalExclusionsFile(File globalExclusionsFile) {
		this.globalExclusionsFile = globalExclusionsFile;
	}

	public void setDependenciesLogFile(File dependenciesLogFile) {
		this.dependenciesLogFile = dependenciesLogFile;
	}

	public void setFileSetId(String id) {
		this.fileSetId = id;
	}

	public void setPomFile(File pomFile) {
		this.pomFile = pomFile;
	}

	@Override
	public void execute() throws BuildException {
		List<String> globalExclusions = parseExclusionPatterns();
		
		
		
		McBridge mcBridge = Bridges.getInstance(getProject());
		CompiledArtifact terminal = mcBridge.readArtifact(pomFile);
		AnalysisArtifactResolution resolution = mcBridge.resolveBuildDependencies(Collections.singletonList(terminal), globalExclusions, Sets.set("test", "provided"), false, true);
		
		// output dependency tree if required
		writeDependencyRelations(resolution);
			
		// create fileset
		exposeAsFileset(mcBridge.getLocalRepository(), resolution.getSolutions());
	}

	private void exposeAsFileset(File localRepository, List<AnalysisArtifact> solutions) {
		FileSet fileSet = new FileSet();
		fileSet.setProject( getProject() );
		fileSet.setDir(localRepository);
		
		
		for (AnalysisArtifact solution: solutions) {
			
			for (Part part: solution.getParts().values()) {
				FileResource fileResource = (FileResource)part.getResource();
				File location = new File(fileResource.getPath());
				String partName = location.getName();
				
				String filePath = PathList.create()
					.pushDottedPath(solution.getGroupId())
					.push(solution.getArtifactId())
					.push(solution.getVersion())
					.push(partName)
					.toFilePath();
				
				fileSet.createInclude().setName(filePath);
			}
		}

		getProject().addReference( fileSetId, fileSet);
	}
	
	private void writeDependencyRelations(AnalysisArtifactResolution resolution) {
		if (dependenciesLogFile == null)
			return;
				
		try (PrintWriter writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dependenciesLogFile), "UTF-8")))) {
			for (AnalysisArtifact depender: resolution.getSolutions()) {
				for (AnalysisDependency dependency: depender.getDependencies()) {
					AnalysisArtifact depended = dependency.getSolution();
					if (depended != null) {
						writer.print(depender.asString());
						writer.print(';');
						writer.println(depended.asString());

					}
				}
			}
		}
		catch (IOException e) {
			throw new BuildException("Error while writing dependencies log file", e);
		}
	}

	private List<String> parseExclusionPatterns() {
		if (globalExclusionsFile != null && globalExclusionsFile.exists()) {
			try {
				List<String> lines = StringTools.readLinesFromInputStream(new FileInputStream(globalExclusionsFile), "UTF-8", true);
		
				List<String> patterns = new ArrayList<>();
	
				for (String line : lines) {
					if (line.trim().length() > 0 && !line.startsWith("#")) {
						patterns.add(line);
					}
				}
				
				return patterns;

			} catch(IOException e) {
				throw new UncheckedIOException("Could not read file "+globalExclusionsFile.getAbsolutePath(), e);
			}
		}
		else {
			System.out.println("no globalexclusions file specified");
			return Collections.emptyList();
		}
	}
}
