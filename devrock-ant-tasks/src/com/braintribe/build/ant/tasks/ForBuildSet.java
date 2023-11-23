// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.ant.types.BuildSet;
import com.braintribe.devrock.mc.api.repository.CodebaseReflection;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;

/**
 * Abstract {@link Task} which operates on multiple artifacts, given as {@link #setBuildSetRefId(String) buildSet}. Given {@link BuildSet} is resolved
 * to artifacts and topologically sorted with dependencies first. The solutions are then passed to the
 * {@link #process(CodebaseAwareBuildDependencyResolutionContract, Set)} method.
 */
public abstract class ForBuildSet extends Task {
	private static Logger log = Logger.getLogger(ForBuildSet.class);
	private String buildSetRefId;
	
	public void setBuildSetRefId(String buildSetRefId) {
		this.buildSetRefId = buildSetRefId;
	}
	
	protected BuildSet getBuildSet() {
		return (BuildSet)getProject().getReference(buildSetRefId);
	}
	
	protected void validate() {
		if (buildSetRefId == null)
			throw new BuildException("you need to supply a buildSetRefId attribute");
	}
	
	@Override
	public void execute() throws BuildException {
		validate();
		
		BuildSet buildSet = getBuildSet();
		
		McBridge mcBridge = Bridges.getInstance(getProject(), buildSet.getCodebaseRoot(), buildSet.getCodebasePattern(), buildSet.getDefaultVersion());
		CodebaseReflection codebaseReflection = mcBridge.getCodebaseReflection();

		AnalysisArtifactResolution buildDependencies = mcBridge.resolveBuildDependencies(buildSet.getRangedTerminals(), buildSet.getArtifactFilter());
		
		if (buildDependencies.getSolutions().isEmpty()) {
			log.warn("no solutions found to process");
			return;
		}

		process(codebaseReflection, buildDependencies);
		
	}
	
	protected abstract void process(CodebaseReflection codebaseReflection, AnalysisArtifactResolution buildDependencies);
}
