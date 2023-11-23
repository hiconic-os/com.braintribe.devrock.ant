package com.braintribe.build.ant.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;

/**
 *
 * simply exposes the publish-tasks' git feature in a separate task 
 * 
 *  <bt:pushToGit message="commit-message">
 *            <pom file="pom.xml"/>
 *    </bt:pushToGit>    	    	
 *	
 * or
 *
 *   <bt:pom file="pom.xml" id="pom" />
 *    ...
 *
 *    <bt:pushToGit message="commit-message">
 *           <pom refid="pom"
 *    </bt:pushToGit>    	    	
 *    
 * to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 * @author pit
 *
 */
public class PushToGitTask extends Task implements GitPublishTrait {
	
	private Pom pom;
	private String message;

	@Configurable @Required
	public void setMessage(String message) {
		this.message = message;		
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
		Pom pom = getPom();
		if (pom == null) {
			throw new BuildException("a pom to write the version to is required");
		}
		
		if (pom.getRefid() != null)
			pom.execute();
		
		File pomFile = pom.getFile();
		if (pomFile == null ) {
			throw new BuildException("no pom file available from pom-task ");
		}
		
		if (!pomFile.exists()) {
			throw new BuildException("pom file [" + pomFile.getAbsolutePath() + "] returned from pom-task doesn't exist");
		}

		File parentDirectory = pomFile.getParentFile();		
		McBridge mcBridge = Bridges.getInstance(getProject());
		
		gitPublish(mcBridge, parentDirectory, message);
	}
	
	
	
}
