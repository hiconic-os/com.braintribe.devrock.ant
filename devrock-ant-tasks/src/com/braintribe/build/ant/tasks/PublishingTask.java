// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.console.ConsoleOutputs.print;
import static com.braintribe.console.ConsoleOutputs.yellow;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.w3c.dom.Document;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.ant.types.RemoteRepository;
import com.braintribe.cfg.Configurable;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.version.Version;
import com.braintribe.utils.paths.PathCollectors;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

/**
 * 
 * to debug: set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 * @author pit
 *
 */
public class PublishingTask extends Task implements PublishTaskTrait, GitPublishTrait, RevisionChangeTrait {
	
	

	private static Logger log = Logger.getLogger(PublishingTask.class);
	
	private Pom pom;

	private RemoteRepository remoteRepository;
	private String usecase;

	private String pathToLocalRepository;

	private String pathToArtifactInLocalRepository;
	private File artifactInLocalRepositoryDirectory;

	private SourceRepositoryKind sourceRepositoryKind = SourceRepositoryKind.git;

	private CompiledArtifact artifact;
	private Ant installDelegateTask;

	private boolean increaseVersion = true;
	private boolean skipGit = false;
	private boolean keepBakOfPackageJson = false;

	@Configurable
	public void setSkipGit(boolean skipGit) {
		this.skipGit = skipGit;
	}

	@Configurable
	public void setIncreaseVersion(boolean increaseVersion) {
		this.increaseVersion = increaseVersion;
	}
	
	/**
	 * can be set to keep a .bak version of the current package.json, for test purposes
	 * 
	 * @param keepBakOfPackageJson - true if a .bak file should be kept
	 */
	@Configurable
	public void setKeepBakOfPackageJson(boolean keepBakOfPackageJson) {
		this.keepBakOfPackageJson = keepBakOfPackageJson;
	}

	public void setSourceRepositoryControl(String control) {
		sourceRepositoryKind = SourceRepositoryKind.valueOf(control);
	}

	public SourceRepositoryKind getSourceRepositoryKind() {
		return sourceRepositoryKind;
	}

	// the pom
	public void addPom(Pom pom) {
		this.pom = pom;
	}
	public Pom getPom() {
		return pom;
	}

	// the use case
	public void setUsecase(String usecase) {
		this.usecase = usecase;
	}

	// the remote repository
	public void addRemoteRepository(RemoteRepository remoteRepository) {
		this.remoteRepository = remoteRepository;
	}
	
	@Override
	public void init() throws BuildException {
		super.init();
		installDelegateTask = new Ant(this);
		installDelegateTask.init();
		installDelegateTask.setTarget("install");
		installDelegateTask.setInheritAll(false);
		installDelegateTask.setAntfile("build.xml");
		
		// pass all properties of this task to the install task as well
		this.getProject().copyUserProperties(installDelegateTask.getProject());
		this.getProject().copyInheritedProperties(installDelegateTask.getProject());
	}

	private String getLocalRepository() {
		File localRepository = Bridges.getInstance(getProject()).getLocalRepository();
		return localRepository.getAbsolutePath();
	}

	private void addRelevantFilesToDeploy(Artifact artifact) {
		if (!artifactInLocalRepositoryDirectory.exists()) {
			print(yellow("WARNING: Nothing was published as directory does not exist: " + artifactInLocalRepositoryDirectory.getAbsolutePath()));
			return;
		}

		addPartsFromDirectory(this, artifactInLocalRepositoryDirectory, artifact);
	}

	private void deploy(Artifact publishArtifact) throws BuildException {
		McBridge mcBridge = Bridges.getInstance(getProject());
		ArtifactResolution resolution = mcBridge.deploy(remoteRepository.getRepository(), publishArtifact);
		
		if (resolution.hasFailed())
			throw new BuildException(resolution.getFailure().stringify());
	}

	@Override
	public void execute() throws BuildException {
		new ColorSupport(getProject()).installConsole();
		
		pathToLocalRepository = getLocalRepository();
		
		Pom pom = getPom();
		if (pom.getRefid() != null)
			pom.execute();

		artifact = pom.getArtifact();

		pathToArtifactInLocalRepository = PathCollectors.filePath.join(pathToLocalRepository.replace('\\', '/'),
				artifact.getGroupId().replace(".", File.separator), artifact.getArtifactId(), artifact.getVersion().asString());
		
		artifactInLocalRepositoryDirectory = new File(pathToArtifactInLocalRepository);

		if (remoteRepository == null) {
			remoteRepository = new RemoteRepository();
			remoteRepository.setUseCase(usecase);
			remoteRepository.setProject(getProject());
		}
		
		McBridge mcBridge = Bridges.getInstance(getProject());

		File pomFile = pom.getFile();
		File workingDirectory = pomFile.getParentFile();

		// retrieve current revision of working copy
		Document pomAsDocument;
		try {
			pomAsDocument = DomParser.load().setNamespaceAware().from(pomFile);
		} catch (DomParserException e) {
			throw new BuildException("cannot load pom [" + pomFile.getAbsolutePath() + "]", e);
		}

		Maybe<Version> readVersionMaybe = getVersion(artifact.getGroupId(), pomAsDocument);
		
		if (readVersionMaybe.isUnsatisfied())
			throw new BuildException(readVersionMaybe.whyUnsatisfied().stringify());
		
		Version readVersion = readVersionMaybe.get();
		
		// M.m.r-qualifier-b
	
		//if (!Optional.ofNullable(readVersion.getQualifier()).orElse("").toLowerCase().equals("pc")) {
		if (!readVersion.isPreliminary()) {
			throw new BuildException(
					"cannot revision [" + readVersion.asString() + "] in pom [" + pomFile.getAbsolutePath() + "] as it is not a valid publishing candidate");
		}
		
		
		
		Integer revision = readVersion.getRevision();
		
		if (revision == null) {
			throw new BuildException(
					"cannot revision [" + readVersion.asString() + "] in pom [" + pomFile.getAbsolutePath() + "] as it has no revision");
		}
		
		//String revisionToPublish = readVersion.getRevision();

		File jsonFile = findJsonPackage(pomFile.getParentFile());
		
		Artifact publishArtifact = Artifact.T.create();
		publishArtifact.setGroupId(artifact.getGroupId());
		publishArtifact.setArtifactId(artifact.getArtifactId());
		publishArtifact.setVersion(artifact.getVersion().asString());

		if (increaseVersion) {
			Version versionToPublish = Version.create(readVersion.getMajor(), readVersion.getMinor(), readVersion.getRevision());
			String versionToPublishStr = versionToPublish.asString();
			
			publishArtifact.setVersion(versionToPublishStr);
		
			// verify if the target version doesn't exist on target repository
			if (mcBridge.artifactExists(remoteRepository.getRepository(), CompiledArtifactIdentification.create(artifact.getGroupId(), artifact.getArtifactId(), versionToPublishStr))) {
				//throw new BuildException("a version [" + versionToPublish + "] already exists for [" + ArtifactIdentification.asString(artifact) + "]");
				throw mcBridge.produceContextualizedBuildException( "a version [" + versionToPublishStr + "] already exists for [" + ArtifactIdentification.asString(artifact) + "]");
			}
			String name = artifact.getArtifactId() + "-" + versionToPublishStr + ".pom";
			log.debug("Increasing artifact version in [" + name + "]");
			
			// change & write pom revision
			writeVersionToPom(pomFile, pomAsDocument, versionToPublish);

			// write json revision
			if (jsonFile != null) {
				writeVersionToJsonPackage( jsonFile, versionToPublishStr, keepBakOfPackageJson);
			}

			pathToArtifactInLocalRepository = PathCollectors.filePath.join(pathToLocalRepository.replace('\\', '/'),
					artifact.getGroupId().replace(".", File.separator), artifact.getArtifactId(), publishArtifact.getVersion());
			artifactInLocalRepositoryDirectory = new File(pathToArtifactInLocalRepository);
			
			// we need to install again using the new version
			try {
				installDelegateTask.execute();
			} catch (Exception e1) {
				writeVersionToPom(pomFile, pomAsDocument, readVersion);
				String errorMessage = "Error while executing (delegate) install task! Reverted version in POM back to '" + readVersion + ".";
				// logging error here, because e1 won't be part of Ant output otherwise
				log.error(errorMessage, e1);
				throw mcBridge.produceContextualizedBuildException(errorMessage, e1);
			}
		}else {
			log.debug("Not increasing the artifact version.");
		}
		
		// deploy to target
		addRelevantFilesToDeploy( publishArtifact);
    
		try {
			deploy(publishArtifact);
		} catch (Exception e1) {
			writeVersionToPom(pomFile, pomAsDocument, readVersion);
			String errorMessage = "Error while executing deploy task! Reverted version in POM back to '" + readVersion + ".";
			// logging error here, because e1 won't be part of Ant output otherwise
			log.error(errorMessage, e1);
			throw mcBridge.produceContextualizedBuildException(errorMessage, e1);
		}

		// create new -pc version 
		if (increaseVersion) {
			int increasedRevision = revision + 1;

			// modify revision on working copy
			Version newLocalVersion = Version.create(readVersion.getMajor(), readVersion.getMinor(), increasedRevision);
			//newLocalVersion.setQualifier("pc");
			newLocalVersion.setQualifier( "rc");
			

			// modify revision to target version
			writeVersionToPom(pomFile, pomAsDocument, newLocalVersion);
			
			// write json revision 
			if (jsonFile != null) {
				writeVersionToJsonPackage( jsonFile, newLocalVersion.asString(), keepBakOfPackageJson);
			}

			if (!skipGit) {
				// commit
				String commitMessage = "Increase revision for publishing from [" + readVersion.asString() + "] to [" + newLocalVersion.asString() + "] for [" + ArtifactIdentification.asString( artifact) + "]";
				gitPublish(mcBridge, workingDirectory, commitMessage);
			}else {
				log.debug("Skipping git.");
			}
		}
		
		System.out.println("Successfully published [" + publishArtifact.asString() + "]");
	}

	public static void main(String[] args) {
		File res = new File( "res");
		File directory = new File(res, "cheap.publishing/packaging.json");
		if (directory.exists() == false) {
			throw new IllegalStateException( directory.getAbsolutePath() + " doesn't exist");
		}
		PublishingTask cpt = new PublishingTask();
		cpt.setKeepBakOfPackageJson(true);
		File jsonPackage = cpt.findJsonPackage(directory);		
		cpt.writeVersionToJsonPackage(jsonPackage, "1.0.1-pc", true);
		
	}
}
