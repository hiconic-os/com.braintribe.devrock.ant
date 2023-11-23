package com.braintribe.build.ant.utils;

import java.util.Arrays;
import java.util.Optional;

import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.PrintStreamConsole;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;

public class DependencyOutputLab {
	public static void main(String[] args) {
		
		AnalysisArtifact fix = artifact("foo.bar", "fix", "1.0");
		AnalysisArtifact fox = artifact("foo.bar", "fox", "1.0");
		AnalysisArtifact ping = artifact("org.faboo", "ping", "1.0");
		AnalysisArtifact pong = artifact("com.come", "pong", "1.0");
		AnalysisArtifact pitsch = artifact("fab.ulous", "pitsch", "1.0.1-pc");
		
		pitsch.setFailure(Reason.create("egal"));
		
		link(fix, fox);
		link(fix, ping);

		link(fox, ping);
		link(fox, pong);
		
		link(ping, pitsch, "[1.0,1.1)");
		
		AnalysisDependency dep = link(pong, null, "pain.ful", "hit", "[1.0,1.1)");
		dep.setFailure(Reason.create("egal"));
		
		AnalysisArtifactResolution resolution = AnalysisArtifactResolution.T.create();
		resolution.getTerminals().add(fix);
		
		resolution.getSolutions().addAll(Arrays.asList(fix, fox, ping, pong));
		
		ConsoleConfiguration.install(new PrintStreamConsole(System.out, true));
		
		ArtifactResolutionUtil.printDependencyTree(resolution);
	}
	
	static void link(AnalysisArtifact depender, AnalysisArtifact dependency) {
		link(depender, dependency, null);
	}
	
	static void link(AnalysisArtifact depender, AnalysisArtifact dependency, String version) {
		link(depender, dependency, null, version);
	}
	
	static void link(AnalysisArtifact depender, AnalysisArtifact dependency, String artifactId, String version) {
		link(depender, dependency, null, artifactId, version);
	}
	
	static AnalysisDependency link(AnalysisArtifact depender, AnalysisArtifact solution, String groupId, String artifactId, String version) {
		AnalysisDependency dependency = AnalysisDependency.T.create();
		dependency.setGroupId(Optional.ofNullable(groupId).orElseGet(() -> solution.getGroupId()));
		dependency.setArtifactId(Optional.ofNullable(artifactId).orElseGet(() -> solution.getArtifactId()));
		dependency.setVersion(Optional.ofNullable(version).orElseGet(() -> solution.getVersion()));
		dependency.setDepender(depender);
		dependency.setSolution(solution);
		dependency.setScope("compile");
		dependency.setType("jar");
		
		depender.getDependencies().add(dependency);
		
		if (solution != null)
			solution.getDependers().add(dependency);
			
		return dependency;
	}
	
	static AnalysisArtifact artifact(String groupId, String artifactId, String version) {
		return artifact(groupId, artifactId, version, "jar");
	}
	
	static AnalysisArtifact artifact(String groupId, String artifactId, String version, String packaging) {
		AnalysisArtifact artifact = AnalysisArtifact.T.create();
		artifact.setGroupId(groupId);
		artifact.setArtifactId(artifactId);
		artifact.setVersion(version);
		artifact.setPackaging(packaging);
		if ("jar".equals( packaging)) {
			artifact.setArchetype( "library");
		}
		return artifact;
	}
}
