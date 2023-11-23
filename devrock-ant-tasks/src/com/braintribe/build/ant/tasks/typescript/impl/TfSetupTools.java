// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript.impl;

import java.io.File;
import java.util.Optional;

import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.generic.reflection.GenericModelException;
import com.braintribe.model.resource.FileResource;

/**
 * Experimental!!! -
 *
 * <h3>Naming</h3>
 * 
 * This is one of few TfSetup helper classes, whose idea is to provide an API for the most common tasks associated with
 * platform-setup-processing. All the public members with static methods (should) have "TfSetup" as their name prefix,
 * and no other class should have that prefix. This way, one can easily find all the available helpers by looking for
 * types matching this pattern.
 * 
 * <h3>(Temporary) Disclaimer</h3>
 * 
 * DO NOT USE OUTSIDE OF THIS ARTIFACT YET SO ALL THESE CAN BE CHANGED WITHOUT RISKING BAD STUFF
 * 
 * This class offers an access to the most common utility tools/methods used in the scope of setup processing.
 * 
 * @author peter.gazdik
 */
public interface TfSetupTools {

	//
	// KEEP THE METHODS SHORT. LONG ONES SHOULD BE IMPLEMENTED ELSEWHERE AND DELEGATED TO.
	//

	// ####################################################
	// ## . . . . . . . . . MC STUFF . . . . . . . . . . ##
	// ####################################################

	/**
	 * @return file for the {@link #getPartLocation(AnalysisArtifact, PartIdentification) location} of given {@link AnalysisArtifact#getParts()
	 *         solution's part} of given {@link PartIdentification type}
	 * 
	 * @throws GenericModelException
	 *             if no part is found or the part has no location
	 * 
	 * @see #findPart(AnalysisArtifact, PartIdentification)
	 */
	static File getPartFile(AnalysisArtifact solution, PartIdentification type) {
		return new File(getPartLocation(solution, type));
	}

	/**
	 * @return {@link Part#getResource() location} of given {@link AnalysisArtifact#getParts() solution's part} of given
	 *         {@link PartIdentification type}
	 * 
	 * @throws GenericModelException
	 *             if no part is found or the part has no location
	 * 
	 * @see #findPart(AnalysisArtifact, PartIdentification)
	 */
	static String getPartLocation(AnalysisArtifact solution, PartIdentification type) {
		return findPartLocation(solution, type) //
				.orElseThrow(
						() -> new GenericModelException("Part not found for solution: " + solution.asString() + ". Requested type: " + type.asString()));
	}

	static Optional<String> findPartLocation(AnalysisArtifact solution, PartIdentification type) {
		return findPart(solution, type) //
				.map(TfSetupTools::getLocation);
	}
	
	static String getLocation(Part part) {
		return ((FileResource)part.getResource()).getPath();
	}

	/**
	 * @return solution {@link Part} of given {@link PartIdentification type}
	 * 
	 * @see PartIdentifications
	 */
	static Optional<Part> findPart(AnalysisArtifact solution, PartIdentification type) {
		return Optional.ofNullable(solution.getParts().get(type.asString()));
	}

}
