// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import com.braintribe.build.ant.types.RemoteRepository;
import com.braintribe.devrock.model.repository.Repository;

/**
 * 
 * mimics the deploy task as defined in maven, but uses malaclypse to install the 
 * compiled artifact. 
 * 
 *  
 * 
 *
 *<artifact:deploy file="dist/lib/${versionedName}.jar">
 *	<pom refid="maven.project"/>
 *	<attach file="${dist}/${versionedName}-sources.jar" classifier="sources"/>
 *	</artifact:deploy>
 *
 *
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 *
 * @author pit
 *
 */
public class DeployTask extends BasicInstallOrDeployTask {
	
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

	@Override
	protected Repository getTargetRepository() {
		if (remoteRepository == null) {
			remoteRepository = new RemoteRepository();
			remoteRepository.setUseCase(usecase);
			remoteRepository.setProject(getProject());
		}

		return remoteRepository.getRepository();
	}
}
