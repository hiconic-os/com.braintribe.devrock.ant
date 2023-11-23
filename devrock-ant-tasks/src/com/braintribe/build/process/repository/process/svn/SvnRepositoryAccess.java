// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.process.repository.process.svn;

import java.io.File;

import com.braintribe.build.process.repository.process.SourceRepositoryAccess;
import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;

public class SvnRepositoryAccess implements SourceRepositoryAccess {
	private SvnInfo svnInfo = new SvnInfo();

	@Override
	public String getBackingUrlOfWorkingCopy(String workingCopy) throws SourceRepositoryAccessException {
		svnInfo.read( workingCopy);
		return svnInfo.getUrl();
	}

	@Override
	public boolean checkout(String url, String workingCopy) throws SourceRepositoryAccessException {
		SvnUtil.checkout(null, url, new File(workingCopy), true);
		return true;
	}

}
