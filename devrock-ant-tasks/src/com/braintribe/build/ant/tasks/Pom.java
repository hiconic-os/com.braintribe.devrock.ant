// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.tasks.validation.PomContentValidatingTask;
import com.braintribe.build.ant.tasks.validation.PomFormatValidatingTask;
import com.braintribe.build.ant.types.Dependency;
import com.braintribe.build.ant.utils.ParallelBuildTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.devrock.model.mc.reason.PomValidationReason;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledDependency;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.version.Version;



/**
 * an ant task the mimics maven's pom task. Keep in mind that it is rather limited, as it only supports
 * the features that we need in Panther and is not a fully featured replacement for maven.<br/><br/>
 * on the other hand, it fully supports the malaclypse ramifications, such as<br/>
 * <b>exclusions</b> : a list of exclusions that is passed down to any dependency<br/>
 * <b>dominants</b> : a list of dependencies that will override any clash resolving done in the depenendency tree<br/>
 * <b>declaration type (FLAT/TRANSIENT)</b>: flat meaning that no transient walk should be done by malaclypse<br/>
 * <br/>
 * to debug: <br/>
 * set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *  
 * @author pit
 *
 */
public class Pom extends Task {

	private static Logger log = Logger.getLogger(Pom.class);
	
	private String id;
	private String refid;
	private File file;
	private boolean validatePom;
	public static String ensureCandidatePomEnvVariable = "DEVROCK_PIPELINE_CANDIDATE_INSTALL";
	public static String ensureCandidatePomProperty = "candidateInstall";
	private String candidateVersionSuffix = "rc";
	
	private CompiledArtifact artifact;
	
		
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getRefid() {
		return refid;
	}
	public void setRefid(String refid) {
		this.refid = refid;
	}
	
	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	@Configurable
	public void setValidatePom(boolean validatePom) {
		this.validatePom = validatePom;
	}
	
	@Initializer("rc")
	@Configurable
	public void setCandidateVersionSuffix(String candidateVersionSuffix) {
		this.candidateVersionSuffix = candidateVersionSuffix;
	}
	
	//
	// redirections
	// 
	public String getArtifactId() {
		if (artifact != null)
			return artifact.getArtifactId();
		return null;
	}
	public String getGroupId() {
		if (artifact != null)
			return artifact.getGroupId();
		return null;
	}
	
	/**
	 * return the version of the artifact as a string
	 * @return - 
	 */
	public String getVersion() {
		if (artifact != null) {
			com.braintribe.model.version.Version version = artifact.getVersion();
			if (version != null) {				
				return version.asString();				
			}
		}
		return null;
	}
	
	/**
	 * return the direct dependencies of the artifact, all while converting them to the 
	 * ant compatible representation of the modelled dependency ge. 
	 * @return -
	 */
	public List<Dependency> getDependencies() {
		List<Dependency> dependencies = new ArrayList<Dependency>();
		if (artifact == null)
			return dependencies;
		for (CompiledDependency compiledDependency : artifact.getDependencies()) {
			Dependency dependency = new Dependency();
			dependency.setGroupId( compiledDependency.getGroupId());
			dependency.setArtifactId( compiledDependency.getArtifactId());			
			dependency.setVersion( compiledDependency.getVersion().asString());			
			dependency.setScope( compiledDependency.getScope());
			dependencies.add(dependency);
		}		
		return dependencies;
	}
	
	/**
	 * return the global exclusions as a list of strings 
	 * caution: it's in standard notation, but the version's missing 
	 * @return - a list of the exclusions
	 */
	public List<String> getExclusions() {
		List<String> result = new ArrayList<String>();		
		Set<ArtifactIdentification> exclusions = artifact.getExclusions();
		if (exclusions != null) {
			for (ArtifactIdentification exclusion : exclusions) {
				result.add( exclusion.getGroupId() + ":" + exclusion.getArtifactId());
			}
		}
		return result;	
	}
	
	/**
	 * return the global dominants as a list of strings,
	 * caution: it's in standard notation, but version's missing
	 * @return - 
	 */
	public List<String> getDominants() {
		List<String> result = new ArrayList<String>();
		return result;
		// TODO : add them if present
		/*
		if (artifact == null)
			return result;
		Collection<Identification> identifications = artifact.getDominants();
		if (identifications != null) {
			for (Identification indentification : identifications) {
				result.add( indentification.getGroupId() + ":" + indentification.getArtifactId());
			}
		}
		return result;
		*/
	}
	
	public CompiledArtifact getArtifact() {
		if (artifact == null) {
			execute();
		}
		return artifact;
	}
	
	@Override
	public void execute() throws BuildException {
		ParallelBuildTools.runGloballySynchronizedRepoRelatedTask(this::_execute);
	}

	private void _execute() throws BuildException {
		
		Project project = getProject();
		
		new ColorSupport(getProject()).installConsole();

		// add to pom registry
		if (id != null) {
			project.addReference( id, this);
		}
		
		//
		// refid passed -> is a reference to the pom stored by the refid as id
		// transfer the artifact, and the pom file (might be used by the dependency walk)
		//
		if (refid != null) {
			//Pom sibling = pomRegistry.getPom(refid);
			Object suspect = project.getReference( refid);
			if (suspect == null) {
				String msg = "reference [" + refid + "] is not set]";
				log.error( msg, null);
				throw new BuildException(msg);
			}
			if (suspect instanceof Pom) {
				Pom sibling = (Pom) suspect;
				artifact = sibling.artifact;
				file = sibling.getFile();				
				return;
			} else {
				String msg = "reference [" + refid + "] doesn't point to a " + Pom.class.getName() + "] but to a [" + suspect.getClass().getName() + "]";
				log.error( msg, null);
				throw new BuildException(msg);
			}
		}	
		
		// get file 
		if (file == null) {
			String msg = "pomfile cannot be null";
			log.error( msg, null);
			throw new BuildException(msg);
		}
		if (file.exists() == false) {
			String msg = "pomfile [" + file.getAbsolutePath() + "] doesn't exist";
			log.error( msg, null);
			throw new BuildException(msg);
		}
		
		// validate pom here?
		if (validatePom) {
			PomFormatValidatingTask pfvt = new PomFormatValidatingTask();
			pfvt.setPomFile( file);
			PomValidationReason formalValidationReason = pfvt.runValidation();
			if (formalValidationReason != null) {
				String msg = "syntactic validation of  [" + file.getAbsolutePath() + "] failed : " + formalValidationReason.stringify();
				log.error( msg);
				throw new BuildException(msg);
			}
			
			PomContentValidatingTask pcvt = new PomContentValidatingTask();
			pcvt.setProject(getProject());
			pcvt.setPomFile( file);
			PomValidationReason contentValidationReason = pcvt.runValidation();
			if (contentValidationReason != null) {
				String msg = "semantic validation of  [" + file.getAbsolutePath() + "] failed : " + contentValidationReason.stringify();
				log.error( msg);
				throw new BuildException(msg);
			}
		}
				
		// actually read the pom  
		try {
			artifact = Bridges.getInstance(getProject()).readArtifact(file);
			
		} catch (Exception e) {
			String msg = "cannot read pom file [" + file.getAbsolutePath() + "]";
			log.error( msg, e);
			throw new BuildException(msg, e);
		}
		
		// neither property nor env-variable set to 'false' (hence "null" means do it) -> tweak pom  		
		String candidatePropertyValue = project.getProperty(ensureCandidatePomProperty);		
		String candidateEnvValue = System.getenv(ensureCandidatePomEnvVariable);
		
		boolean ensure = false;
		if (candidatePropertyValue != null) {
			if ("true".equals( candidatePropertyValue)) {
				ensure = true;
			}
			else {
				ensure = false;
			}
		}
		else if (candidateEnvValue != null) {
			if ("true".equals( candidateEnvValue)) {
				ensure = true;
			}
			else {
				ensure = false;
			}
		}
		else {
			ensure = true; // ? default ? 
		}
								
		if (ensure) {
			ensureCandidatePom( artifact);
		}
		
		
		// expose artifact groupId, artifactId, version
		getProject().setProperty( id + ".groupId", artifact.getGroupId());
		getProject().setProperty( id + ".artifactId", artifact.getArtifactId());
		getProject().setProperty( id + ".version", artifact.getVersion().asString());
		
		// export all properties
		for (Map.Entry<String,String> entry : artifact.getProperties().entrySet()) {								
			getProject().setProperty( id + ".properties." + entry.getKey(), entry.getValue());			
		}							
	}
	
	/**
	 * makes sure that the version is a 'preliminary' version, otherwise modifies it to be one.
	 * @param artifact - the {@link CompiledArtifact}
	 */
	private void ensureCandidatePom(CompiledArtifact artifact) {
		Version version = artifact.getVersion();
		if (version.isPreliminary()) {
			return;
		}
		Version candidateVersion = Version.from(version);
		// raises
		Integer revision = version.getRevision();
		if (revision != null) {
			candidateVersion.setQualifier(candidateVersionSuffix);
			artifact.setVersion(candidateVersion);
		}
		else {		
			// error condition.. 
			String msg = "cannot create raised candidate-version as no revision contained in [" + version.asString() +"] of artifact:" + artifact.getArtifactId();
			log.error(msg);
			throw new BuildException( msg);
		}		
	}
}
