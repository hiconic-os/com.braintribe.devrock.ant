package com.braintribe.model.artifact.analysis;

import java.util.List;
import java.util.Set;

import com.braintribe.gm.model.reason.HasFailure;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface AnalysisArtifactResolution extends HasFailure {
	EntityType<AnalysisArtifactResolution> T = EntityTypes.T(AnalysisArtifactResolution.class);
	
	String terminals = "terminals";
	String solutions = "solutions";
	String clashes = "clashes";
	String unresolvedDependencies = "unresolvedDependencies";
	String incompleteArtifacts = "incompleteArtifacts";

	List<AnalysisTerminal> getTerminals();
	void setTerminals(List<AnalysisTerminal> terminals);
	
	List<AnalysisArtifact> getSolutions();
	void setSolutions(List<AnalysisArtifact> solutions);

	List<DependencyClash> getClashes();
	void setClashes(List<DependencyClash> clashes);
	
	Set<AnalysisDependency> getUnresolvedDependencies();
	void setUnresolvedDependencies(Set<AnalysisDependency> unresolvedDependencies);
	
	Set<AnalysisDependency> getFilteredDependencies();
	void setFilteredDependencies(Set<AnalysisDependency> unresolvedDependencies);
	
	Set<AnalysisArtifact> getIncompleteArtifacts();
	void setIncompleteArtifacts(Set<AnalysisArtifact> incompleteArtifacts);
	
}
