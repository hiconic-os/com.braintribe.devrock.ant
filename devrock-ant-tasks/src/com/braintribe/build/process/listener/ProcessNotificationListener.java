// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.process.listener;

/**
 * listener for messages from the process handlers (svn util, other command line)
 * @author pit
 *
 */
public interface ProcessNotificationListener {
	void acknowledgeProcessNotification( MessageType messageType, String msg);

}
