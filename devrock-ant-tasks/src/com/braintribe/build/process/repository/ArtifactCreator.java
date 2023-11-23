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
import com.braintribe.build.process.repository.manipulators.BuildRenamer;
import com.braintribe.build.process.repository.manipulators.DotProjectRenamer;
import com.braintribe.build.process.repository.manipulators.PomRenamer;
import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;
import com.braintribe.build.process.repository.process.svn.SvnUtil;
import com.braintribe.build.process.repository.utils.FileUtils;
import com.braintribe.build.process.repository.utils.NameUtils;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.Artifact;
import com.braintribe.model.artifact.processing.version.VersionProcessor;


public class ArtifactCreator  {
	private static Logger log = Logger.getLogger(ArtifactCreator.class);
	
	private static String getArtifactSvnUrl( Artifact artifact){
		String group = artifact.getGroupId();
		String id = artifact.getArtifactId();
		String version = VersionProcessor.toString( artifact.getVersion());
		
		return NameUtils.getArtifactSvnPath(group, id, version);		
	}

	public static void create(  ProcessNotificationListener listener, Artifact artifact, Artifact template, String svnPath, String svnUrl) throws SourceRepositoryAccessException {
		String group = artifact.getGroupId();
		String id = artifact.getArtifactId();
		String version = VersionProcessor.toString( artifact.getVersion());
		
		
		String newArtifactUrl = NameUtils.getArtifactSvnPath(group, id, version);
		
		String creatingMessage = MessageFormat.format( "Creating {0}:{1}-{2}", group, id, version);
		listener.acknowledgeProcessNotification(MessageType.info, creatingMessage);
		
		String basePath = svnPath + "/" + newArtifactUrl;
		
		
		// 
		File homeDirFile = new File( basePath);
		
		if (
				(homeDirFile.exists() == false) &&
				(homeDirFile.mkdirs() == false)
			){
			String msg = "cannot generate directories for [" + basePath + "].";
			log.error(msg, null);
			throw new SourceRepositoryAccessException(msg);
		}
		
		
		 
		
		
		// grap the build.xml, pom.xml, .project as defined.. 
		File tempDir = FileUtils.createTmpDirectory();		
		
		String svnRepo = getArtifactSvnUrl(template);
		// 			
		String [] names = new String [] {"build.xml", "pom.xml", ".project", ".svnignore", ".classpath", "container.xml", "doc.txt"};
		for (String name : names) {
			listener.acknowledgeProcessNotification(MessageType.info, "Getting file [" + name + "]");
			try {
				SvnUtil.export( listener, svnUrl + "/" + svnRepo + "/" + name, new File(tempDir, name));
				FileUtils.copy( new File( tempDir, name), new File( basePath, name ));
			} catch (Exception e) {
				listener.acknowledgeProcessNotification(MessageType.info, "Cannot get file [" + name + "]");
			}
		}		
		FileUtils.deleteDirectory(tempDir);
		
		String adaptMessage = MessageFormat.format( "Adjusting files after copying defaults for {0}:{1}-{2}", group, id, version);
		listener.acknowledgeProcessNotification(MessageType.info, adaptMessage);
		
				
		// pom.xml
		PomRenamer pomRenamer = new PomRenamer();
		pomRenamer.setGroupId( group);
		pomRenamer.setArtifactId( id);
		pomRenamer.setVersion( version);
		File pomFile = new File( basePath, "pom.xml");
		pomRenamer.load( pomFile);
		pomRenamer.adjust();
		pomRenamer.store( pomFile);
		
		
		// .project		
		DotProjectRenamer projectRenamer = new DotProjectRenamer();
		projectRenamer.setProjectName( id + "-" + version);
		File dotProjectFile = new File( basePath, ".project");
		projectRenamer.load( dotProjectFile);
		projectRenamer.adjust();
		projectRenamer.store( dotProjectFile);
		
		// build file
	
		BuildRenamer buildRenamer = new BuildRenamer();
		buildRenamer.setProjectName( id);
		File buildFile = new File( basePath, "build.xml");
		buildRenamer.load(buildFile);
		buildRenamer.adjust();
		buildRenamer.store( buildFile);
		
		// create src directory 
		File srcFile = new File( basePath, "src");
		srcFile.mkdirs();
				
	}
}
