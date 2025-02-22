package com.braintribe.build.ant.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.process.ProcessException;
import com.braintribe.build.process.ProcessExecution;
import com.braintribe.build.process.ProcessResults;
import com.braintribe.logging.Logger;
import com.braintribe.utils.StringTools;

public interface GitPublishTrait {
	static Logger log = Logger.getLogger(GitPublishTrait.class);

	default void gitPublish(McBridge mcBridge, File workingDirectory, String commitMessage) {
	
		List<String> cmd = new ArrayList<String>();

		cmd.add( "git");
		cmd.add("commit");
		cmd.add("-m");
		cmd.add("\"" + commitMessage + "\"");
		cmd.add("pom.xml");
		
		log.debug("Running git command: [" + StringTools.join(" ", cmd) + "]");

		try {
			ProcessResults result = ProcessExecution.runCommand(cmd, workingDirectory, null, null);
			if (result.getRetVal() != 0) {
				System.err.println(result.getErrorText());
				throw mcBridge.produceContextualizedBuildException(
						"deploy succeeded, but cannot commit changed revision on pom.xml in [" + workingDirectory.getAbsolutePath() + "]");
			}
		} catch (ProcessException e) {
			throw mcBridge.produceContextualizedBuildException(
					"deploy succeeded, but cannot commit changed revision on pom.xml in [" + workingDirectory.getAbsolutePath() + "]", e);
		}

		cmd = new ArrayList<String>();
		cmd.add("git");
		cmd.add("push");
		
		log.debug("Running git command: [" + StringTools.join(" ", cmd) + "]");
		
		try {
			ProcessResults result = ProcessExecution.runCommand(cmd, workingDirectory, null, null);
			if (result.getRetVal() != 0) {
				System.err.println(result.getErrorText());
				throw mcBridge.produceContextualizedBuildException(
						"deploy succeeded, but cannot push changed revision on pom.xml in [" + workingDirectory.getAbsolutePath() + "]");
			}
		} catch (ProcessException e) {
			throw mcBridge.produceContextualizedBuildException(
					"deploy succeeded, but cannot push changed revision on pom.xml in [" + workingDirectory.getAbsolutePath() + "]", e);
		}
	}
}
