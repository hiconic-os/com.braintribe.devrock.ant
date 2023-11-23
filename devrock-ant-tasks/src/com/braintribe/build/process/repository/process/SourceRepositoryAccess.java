// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.process.repository.process;

/**
 * interface that represents the (current) access that is required in the plugins to either git or svn
 * @author pit
 *
 */
public interface SourceRepositoryAccess {

	String getBackingUrlOfWorkingCopy(String workingCopy) throws SourceRepositoryAccessException;
	boolean checkout( String url, String workingCopy) throws SourceRepositoryAccessException;
	
}
