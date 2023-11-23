// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.listener;

import static com.braintribe.utils.lcd.CollectionTools2.newList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;

import com.braintribe.common.lcd.UnknownEnumException;

/**
 * @author peter.gazdik
 */
public class EventCollector implements BuildListener {

	private final Map<ThreadGroup, List<EventEntry>> eventEntries = new ConcurrentHashMap<>();

	private final BuildLogger mainThreadLogger;
	private final Thread mainThread;

	public EventCollector(BuildLogger mainThreadLogger) {
		this.mainThreadLogger = mainThreadLogger;
		this.mainThread = Thread.currentThread();

	}

	@Override
	public void buildStarted(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.buildStarted, event));
	}

	@Override
	public void buildFinished(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.buildFinished, event));
	}

	@Override
	public void targetStarted(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.targetStarted, event));
	}

	@Override
	public void targetFinished(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.targetFinished, event));
	}

	@Override
	public void taskStarted(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.taskStarted, event));
	}

	@Override
	public void taskFinished(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.taskFinished, event));
	}

	@Override
	public void messageLogged(BuildEvent event) {
		addEventEntry(new EventEntry(EventType.messageLogged, event));
	}

	private void addEventEntry(EventEntry eventEntry) {
		ThreadGroup group = resolveThreadGroup();
		if (group == mainThread.getThreadGroup()) {
			eventEntry.applyOn(mainThreadLogger);
			return;
		}

		List<EventEntry> list = eventEntries.computeIfAbsent(group, g -> Collections.synchronizedList(newList()));
		list.add(eventEntry);
	}

	public List<EventEntry> removeEntries() {
		ThreadGroup key = resolveThreadGroup();
		return eventEntries.remove(key);
	}

	private ThreadGroup resolveThreadGroup() {
		return Thread.currentThread().getThreadGroup();
	}

	private enum EventType {
		buildStarted,
		buildFinished,
		targetStarted,
		targetFinished,
		taskStarted,
		taskFinished,
		messageLogged,
	}

	public static class EventEntry {
		EventType type;
		BuildEvent event;

		private EventEntry(EventType type, BuildEvent event) {
			this.type = type;
			this.event = event;
		}

		public void applyOn(BuildListener listener) {
			switch (type) {
				case buildFinished:
					listener.buildFinished(event);
					return;
				case buildStarted:
					listener.buildStarted(event);
					return;
				case messageLogged:
					listener.messageLogged(event);
					return;
				case targetFinished:
					listener.targetFinished(event);
					return;
				case targetStarted:
					listener.targetStarted(event);
					return;
				case taskFinished:
					listener.taskFinished(event);
					return;
				case taskStarted:
					listener.taskStarted(event);
					return;
				default:
					throw new UnknownEnumException(type);
			}
		}
	}

}
