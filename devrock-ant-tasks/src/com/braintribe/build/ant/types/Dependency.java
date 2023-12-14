// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.types;


import org.apache.tools.ant.BuildException;

import com.braintribe.model.artifact.compiled.CompiledDependency;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.version.VersionExpression;

/**
 * helper class to enable the bt:dependencies task to handle
 * child elements for virtual pom files. 
 * 
 * @author Pit
 *
 */
public class Dependency {
	private String groupId;
	private String artifactId;
	private String version;
	private String scope;
	private String exclusions;
	
	public String getGroupId() {
		return groupId;
	}
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}
	public String getArtifactId() {
		return artifactId;
	}
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getExclusions() {
		return exclusions;
	}
	public void setExclusions(String exclusions) {
		this.exclusions = exclusions;
	}

	/**
	 * Convenience method which sets {@link #groupId}, {@link #artifactId} and {@link #version}. 
	 * @param condensedArtifactName the condensed artifact name, e.g. <code>com.braintribe:Example#1.2.3</code>
	 */
	public void setCondensedArtifactName(String condensedArtifactName) {
		try {
			VersionedArtifactIdentification artifact = VersionedArtifactIdentification.parse(condensedArtifactName);
			groupId = artifact.getGroupId();
			artifactId = artifact.getArtifactId();
			version = artifact.getVersion();
		} catch (Exception e) {
			String msg = "Can't parse condensed name '" + condensedArtifactName + "'.";
			throw new BuildException( msg, e);
		} 
	}
	public String getScope() {
		return scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	@Override
	public String toString(){
		return groupId + ":" + artifactId + "#" + version;
	}
	
	public CompiledDependency asCompiledDependency() {
		CompiledDependency result = CompiledDependency.create(getGroupId(), getArtifactId(), VersionExpression.parse(getVersion()), getScope());

		if (exclusions != null)
			for (String excludedArtifact : exclusions.split(","))
				result.getExclusions().add(ArtifactIdentification.parse(excludedArtifact));

		return result;
	}
}
