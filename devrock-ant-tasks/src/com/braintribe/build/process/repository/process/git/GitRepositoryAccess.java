// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.process.repository.process.git;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.braintribe.build.process.ProcessException;
import com.braintribe.build.process.ProcessExecution;
import com.braintribe.build.process.ProcessResults;
import com.braintribe.build.process.repository.process.SourceRepositoryAccess;
import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;

/**
 * git - info
 * git config --get remote.origin.url
 * i.e. 
 * $ git config --get remote.origin.url -> https://github.com/torvalds/linux.git
 * 
 * git checkout 
 * git clone <url> <target>
 * i.e.
 * $ git clone https://github.com/libgit2/libgit2 mylibgit
 * @author pit
 *
 */
public class GitRepositoryAccess implements SourceRepositoryAccess {

	@Override
	public String getBackingUrlOfWorkingCopy(String workingCopy) throws SourceRepositoryAccessException {
		List<String> cmd = new ArrayList<String>();
		cmd.add( "git");
		cmd.add( "remote");
		cmd.add( "get-url");
		cmd.add( "origin");
		try {
			ProcessResults runCommand = ProcessExecution.runCommand(cmd, new File(workingCopy), null, null);
			return runCommand.getNormalText();
		} catch (ProcessException e) {
			throw new SourceRepositoryAccessException("cannot determine GIT's backing url of [" + workingCopy + "]", e);
		}
		//throw new SourceRepositoryAccessException("this feature is currently not implemented yet");		
	}

	@Override
	public boolean checkout(String url, String workingCopy) throws SourceRepositoryAccessException {
		List<String> cmd = new ArrayList<String>();
		cmd.add( "git");
		cmd.add( "clone");
		cmd.add( url);
		cmd.add( workingCopy);
		try {
			ProcessResults runCommand = ProcessExecution.runCommand(cmd, null, null, null);
			if (runCommand.getRetVal() == 0)
				return true;
			else
				return false;
		} catch (ProcessException e) {
			throw new SourceRepositoryAccessException("cannot determine GIT's backing url of [" + workingCopy + "]", e);
		}
		//throw new SourceRepositoryAccessException("this feature is currently not implemented yet");		
	}

}
