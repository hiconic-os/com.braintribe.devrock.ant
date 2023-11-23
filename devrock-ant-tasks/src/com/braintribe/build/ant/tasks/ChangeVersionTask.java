package com.braintribe.build.ant.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.version.Version;

/**
 * simple task that gets two arguments : the pom and the version to write to it (and ev the json-package file if present)
 * 
 * <bt:writeVersionToPom version="1.0.1" keepBackOfPackageJson="true">
 * 	  <pom refid="pom" />
 * </bt:writeVersionToPom>
 * 
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 * @author pit
 *
 */
public class ChangeVersionTask extends Task implements RevisionChangeTrait {

	private Pom pom;
	private boolean keepBakOfPackageJson = false;
	private CompiledArtifact artifact;
	
	private Version version;
	
	/**
	 * can be set to keep a .bak version of the current package.json, for test purposes
	 * 
	 * @param keepBakOfPackageJson - true if a .bak file should be kept
	 */
	@Configurable
	public void setKeepBakOfPackageJson(boolean keepBakOfPackageJson) {
		this.keepBakOfPackageJson = keepBakOfPackageJson;
	}
	
	/**
	 * set the version to write to the pom / json
	 * @param versionAsString
	 */
	@Configurable @Required
	public void setVersion(String versionAsString) {
		this.version = Version.parse(versionAsString);
	}
	
	// the pom
	public void addPom(Pom pom) {
		this.pom = pom;
	}
	public Pom getPom() {
		return pom;
	}
	
	
	@Override
	public void execute() throws BuildException {
		new ColorSupport(getProject()).installConsole();
		
		if (version == null) {
			throw new BuildException("a version to write to the pom/json-package is required");
		}	
		
		Pom pom = getPom();
		if (pom == null) {
			throw new BuildException("a pom to write the version to is required");
		}
		
		if (pom.getRefid() != null)
			pom.execute();
		
		File pomFile = pom.getFile();		

		artifact = pom.getArtifact();
		
		Version currentVersion = artifact.getVersion();
				
		ConsoleOutputs.println("changing found version [" + currentVersion.asString() + "] to new version [" + version.asString() + "]" );

		// modify revision to target version
		writeVersionToPom(pomFile, version);
		
		// write json revision 
		File jsonFile = findJsonPackage(pomFile.getParentFile());		
		if (jsonFile != null) {
			writeVersionToJsonPackage( jsonFile, version, keepBakOfPackageJson);
		}
		super.execute();
	}

	
}
