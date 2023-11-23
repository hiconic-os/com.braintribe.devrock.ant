// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.types.RemoteRepository;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.utils.paths.PathCollectors;

/**
 * actually this is the deploy task - i.e. taking something from a local repository and upload it 
 * - with all bells & whistles (hashes, metadata integration) to a remote repository.
 * 
 *   <bt:publish-direct>
 *   	<bt:pom file="pom.xml"/>
 *   </bt:publish-direct>
 * 
 * or 
 *   <bt:publish-direct artifact="com.braintribe.devrock.test:t#1.0.1">    	
 *   </bt:publish-direct
 * 
 * to debug: set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 * @author pit
 *
 */
public class DirectPublishingTask extends AbstractTransferTask implements PublishTaskTrait {
	
	private RemoteRepository remoteRepository;
	private String usecase;


	// the use case
	public void setUsecase(String usecase) {
		this.usecase = usecase;
	}

	// the remote repository
	public void addRemoteRepository(RemoteRepository remoteRepository) {
		this.remoteRepository = remoteRepository;
	}
	
	private String getLocalRepository() {
		File localRepository = Bridges.getInstance(getProject()).getLocalRepository();
		return localRepository.getAbsolutePath();
	}

	@Override
	protected Repository getTargetRepository() {
		if (remoteRepository == null) {
			remoteRepository = new RemoteRepository();
			remoteRepository.setUseCase(usecase);
			remoteRepository.setProject(getProject());
		}

		return remoteRepository.getRepository();
	}
	
	
	@Override
	protected void addParts(Artifact transferArtifact) {
		VersionedArtifactIdentification artifact = getVersionedArtifactIdentification();
		
		String pathToLocalRepository = getLocalRepository();

		String pathToArtifactInLocalRepository = PathCollectors.filePath.join(pathToLocalRepository.replace('\\', '/'),
				artifact.getGroupId().replace(".", File.separator), artifact.getArtifactId(), artifact.getVersion());
		
		File artifactInLocalRepositoryDirectory = new File(pathToArtifactInLocalRepository);
		
		if (!artifactInLocalRepositoryDirectory.exists()) {
			return;
		}

		addPartsFromDirectory(this, artifactInLocalRepositoryDirectory, transferArtifact);
	}
}
