package com.braintribe.model.artifact.analysis;

import com.braintribe.gm.model.reason.HasFailure;
import com.braintribe.model.artifact.compiled.CompiledDependency;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.version.Version;

/**
 * represents a dependency
 *  
 * @author pit
 *
 */
public interface AnalysisDependency extends VersionedArtifactIdentification, PartIdentification, HasFailure, AnalysisTerminal {
	
	EntityType<AnalysisDependency> T = EntityTypes.T(AnalysisDependency.class);

	String origin = "origin";
	String scope = "scope";
	String solution = "solution";
	String depender = "depender";
	String optional = "optional";
	
	/**
	 * @return - the {@link CompiledDependency} that is the origin of this {@link AnalysisDependency}
	 */
	CompiledDependency getOrigin();
	void setOrigin( CompiledDependency origin);
	
	/**
	 * @return - the single {@link AnalysisArtifact} that this {@link AnalysisDependency} points to (after clash resolving if involved)
	 */
	AnalysisArtifact getSolution();
	void setSolution( AnalysisArtifact solution);
	
	/**
	 * @return - the {@link AnalysisArtifact} that contained the {@link AnalysisDependency} if there was such an 
	 * artifact. It may be null in case this dependency is a terminal dependency which means that it was
	 * used as a {@link CompiledTerminal} in a resolution and occurs as {@link AnalysisTerminal} on {@link AnalysisArtifactResolution#getTerminals()}
	 */
	AnalysisArtifact getDepender();
	void setDepender( AnalysisArtifact depender);
	
	/**	 
	 * @return - the scope as declared in the pom
	 */
	String getScope();
	void setScope( String scope);
	
	/**	 
	 * @return - optional as declared in the pom
	 */
	boolean getOptional();
	void setOptional( boolean optional);
	
	
	@Override
	default String asString() {		
		if (getType() != null) {
			return VersionedArtifactIdentification.super.asString() + "/" + PartIdentification.super.asString();
		}
		else {
			return VersionedArtifactIdentification.super.asString();
		}
	}
	
	/**
	 * helper to create a {@link AnalysisDependency} from a {@link CompiledDependency}
	 * @param dependency - the {@link CompiledDependency}
	 * @return - the transposed {@link AnalysisDependency}
	 */
	static AnalysisDependency from(CompiledDependency dependency) {
		AnalysisDependency analysisDependency = AnalysisDependency.T.create();
		analysisDependency.setGroupId(dependency.getGroupId());
		analysisDependency.setArtifactId(dependency.getArtifactId());
		analysisDependency.setVersion(dependency.getVersion().asString());
		analysisDependency.setScope(dependency.getScope());
		analysisDependency.setOptional(dependency.getOptional());
		analysisDependency.setOrigin(dependency);
		return analysisDependency;
	}
	
	/**
	 * @param other - the other {@link AnalysisDependency}
	 * @return - the comparison value {@link ArtifactIdentification} and {@link Version}
	 */
	default int compareTo( AnalysisDependency other) {
		int result = ((ArtifactIdentification) this).compareTo(other);
		if (result != 0)
			return result;
		Version thisVersion = Version.parse( this.getVersion());
		Version otherVersion = Version.parse( other.getVersion());
		return thisVersion.compareTo(otherVersion); 
	}
}
