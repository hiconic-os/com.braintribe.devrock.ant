// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository;

import java.io.File;
import java.text.MessageFormat;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.build.process.repository.manipulators.DotProjectRenamer;
import com.braintribe.build.process.repository.manipulators.PomRenamer;
import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;
import com.braintribe.build.process.repository.process.svn.SvnInfo;
import com.braintribe.build.process.repository.process.svn.SvnUtil;
import com.braintribe.build.process.repository.utils.FileUtils;
import com.braintribe.build.process.repository.utils.NameUtils;
import com.braintribe.build.process.repository.utils.Path;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.Artifact;
import com.braintribe.model.artifact.processing.version.VersionProcessor;

public class ArtifactCloner {
	private static Logger log = Logger.getLogger(ArtifactCloner.class); 

	public static void clone( ProcessNotificationListener listener,  Artifact artifact, String cloneVersion, String svnPath, String svnUrl) throws SourceRepositoryAccessException {
		
		String group = artifact.getGroupId();
		String id = artifact.getArtifactId();
		String version = VersionProcessor.toString( artifact.getVersion());
		
		
		String basePath = NameUtils.getArtifactSvnPath(group, id, version);		
		
		String cloningMessage = MessageFormat.format( "Cloning {0}:{1}-{2} to {0}:{1}-{3}", group, id, version, cloneVersion);
		
		if (svnPath.length() > 0) {
			basePath = svnPath + "/" + basePath;
		}
		
		SvnInfo baseInfo = new SvnInfo();
		baseInfo.setListener(listener);
		baseInfo.read( basePath);
		String baseUrl = baseInfo.getUrl();
		
		Path path = new Path( baseUrl, "/"); 
		
		path.pop();
		path.push( cloneVersion);
		
		String cloneUrl = path.toString();
		
		// funny enough, this always crashes if the url doesn't exist.. 
		boolean exists = false;
		try {
			exists = SvnUtil.exists(listener, cloneUrl);
		} 
		catch (Exception e) {			
		}
		
		if (exists) {
			String message = MessageFormat.format( "Cannot clone {0}:{1}-{2} to {0}:{1}-{3} as target already exists", group, id, version, cloneVersion);
			log.error( message, null);
			throw new SourceRepositoryAccessException( message);
		}
		
		listener.acknowledgeProcessNotification(MessageType.info, cloningMessage);

		SvnUtil.branch(listener, baseUrl, cloneUrl, cloningMessage);
		
		
		String adaptMessage = MessageFormat.format( "Adjusting files after cloning {0}:{1}-{2} to {0}:{1}-{3}", group, id, version, cloneVersion);
		listener.acknowledgeProcessNotification(MessageType.info, adaptMessage);
				
		// modify pom.xml, .project
		File tempDir = FileUtils.createTmpDirectory();
		SvnUtil.export( listener, cloneUrl, tempDir);
		
		// pom.xml
		PomRenamer pomRenamer = new PomRenamer();
		pomRenamer.setVersion( cloneVersion);
		File pomFile = new File(tempDir, "pom.xml");
		pomRenamer.load( pomFile);
		pomRenamer.adjust();
		pomRenamer.store( pomFile);
		
		
		// .project		
		DotProjectRenamer projectRenamer = new DotProjectRenamer();
		projectRenamer.setProjectName( id + "-" + cloneVersion);
		File dotProjectFile = new File( tempDir, ".project");
		projectRenamer.load( dotProjectFile);
		projectRenamer.adjust();
		projectRenamer.store( dotProjectFile);
		
		// build file
		/*
		BuildRenamer buildRenamer = new BuildRenamer();
		buildRenamer.setProjectName( id);
		File buildFile = new File( tempDir, "build.xml");
		buildRenamer.load(buildFile);
		buildRenamer.adjust();
		buildRenamer.store( buildFile);
		*/
		
		// commit the changes
		SvnUtil.commit(listener, tempDir, adaptMessage);
		FileUtils.deleteDirectory(tempDir);
		
		String checkOutMessage = MessageFormat.format( "Checking out {0}:{1}-{3}", group, id, version, cloneVersion);
		listener.acknowledgeProcessNotification(MessageType.info, checkOutMessage);
		String clonePath = NameUtils.getArtifactSvnPath(group, id, cloneVersion);		
		SvnUtil.checkout(listener, cloneUrl, new File(svnPath, clonePath), true);
		
			
	}
	
}
