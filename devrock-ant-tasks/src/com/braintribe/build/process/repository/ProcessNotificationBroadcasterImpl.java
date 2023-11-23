// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.process.repository;

import java.util.HashSet;
import java.util.Set;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationBroadcaster;
import com.braintribe.build.process.listener.ProcessNotificationListener;

public class ProcessNotificationBroadcasterImpl implements ProcessNotificationBroadcaster, ProcessNotificationListener {
	Set<ProcessNotificationListener> listeners;

	@Override
	public void addListener(ProcessNotificationListener listener) {
		if (listeners == null)
			listeners = new HashSet<ProcessNotificationListener>();
		listeners.add( listener);

	}

	@Override
	public void removeListener(ProcessNotificationListener listener) {
		if (listeners == null)
			return;
		listeners.remove(listener);
		if (listeners.size() == 0)
			listeners = null;

	}

	@Override
	public void acknowledgeProcessNotification(MessageType messageType, String msg) {	
		if (listeners == null)
			return;
		for (ProcessNotificationListener listener : listeners) {
			listener.acknowledgeProcessNotification(messageType, msg);
		}
	}
		

}
