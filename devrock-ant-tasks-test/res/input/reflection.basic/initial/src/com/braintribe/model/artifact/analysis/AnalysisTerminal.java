package com.braintribe.model.artifact.analysis;

import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.generic.annotation.Abstract;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

/**
 * represents a dependency
 *  
 * @author pit
 *
 */
@Abstract
public interface AnalysisTerminal extends ArtifactIdentification {
	
	EntityType<AnalysisTerminal> T = EntityTypes.T(AnalysisTerminal.class);
	
}
