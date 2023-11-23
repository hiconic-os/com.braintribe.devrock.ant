// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks.malaclypse;

import java.util.HashSet;
import java.util.Set;

import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.essential.ArtifactIdentification;

public class ArtifactExclusionList {

	private static Logger log = Logger.getLogger(ArtifactExclusionList.class);
	
	private Set<ArtifactIdentification> exclusions;

	public Set<ArtifactIdentification> getExclusions() {
		return exclusions;
	}

	public void setExclusions(Set<ArtifactIdentification> exclusions) {
		this.exclusions = exclusions;
	}
	
	
	public ArtifactExclusionList( String asString) {
		String [] exclusionsAsString = asString.split( "\n");
		exclusions = new HashSet<>( exclusionsAsString.length);
		for (String exclusionAsString : exclusionsAsString) {
			
			if (exclusionAsString.startsWith( "#"))
				continue;
			exclusionAsString = exclusionAsString.trim();
			if (exclusionAsString.length() > 0) {
				try {
					ArtifactIdentification artifactIdentification = ArtifactIdentification.parse(exclusionAsString);
					exclusions.add( artifactIdentification);
				} catch (UnsupportedOperationException e) {
					log.error( "cannot add exclusion from String [" + exclusionAsString + "] as it's invalid", null);
				}
			}
		}
	}
}
