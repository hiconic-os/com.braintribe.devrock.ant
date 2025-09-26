// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import org.apache.tools.ant.BuildException;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.devrock.model.repository.Repository;

/**
 * 
 * mimics the install task as defined in maven, but uses malaclypse to install the 
 * compiled artifact. 
 * 
 *  
 * 
 *
 *<artifact:install file="dist/lib/${versionedName}.jar">
 *	<pom refid="maven.project"/>
 *	<attach file="${dist}/${versionedName}-sources.jar" classifier="sources"/>
 *	</artifact:install>
 *
 *
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 *
 * @author pit
 *
 */
public class InstallTask extends BasicInstallOrDeployTask {
	
	@Override
	protected Repository getTargetRepository() {
		McBridge bridge = Bridges.getInstance(getProject());
		
		Repository repository = bridge.getRepositoryConfiguration().getInstallRepository();
		
		if (repository != null)
			return repository;
		
		repository = bridge.getRepository("local");
		
		if (repository != null)
			return repository;
		
		throw new BuildException("No install repository configured in RepositoryConfiguration.");
	}
}
