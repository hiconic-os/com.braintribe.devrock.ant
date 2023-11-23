// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tools.ant.DemuxOutputStream;
import org.apache.tools.ant.Main;
import org.apache.tools.ant.Project;

/**
 * Improvement over the default {@link DemuxOutputStream} used by ANT's {@link Main#runBuild main} method, this one allows registration of an Ant
 * {@link Project} for a given thread.
 * 
 * This is also relevant in a single-threaded build, as even there the default DemuxOutputStream is not aware of the sub-task.
 * 
 * Output WITHOUT this (warning are logged for [bt:transitive-build]):
 * 
 * <pre>
 *  compile:
 *    ...
 *    [bt:transitive-build] Note: Some input files use or override a deprecated API.
 *    [bt:transitive-build] Note: Recompile with -Xlint:deprecation for details.
 * </pre>
 * 
 * Output WITH this (warnings are logged for [javac]):
 * 
 * <pre>
 *  compile:
 *    ...
 *    [javac] Note: Some input files use or override a deprecated API.
 *    [javac] Note: Recompile with -Xlint:deprecation for details.
 * </pre>
 * 
 * @author peter.gazdik
 */
public class DrDemuxOutputStream extends OutputStream {

	private final Map<ThreadGroup, DemuxOutputStream> streams = new ConcurrentHashMap<>();

	private final ThreadGroup mainThreadGroup;
	private final DemuxOutputStream mainDemuxOs;

	private final boolean isErrorStream;

	public DrDemuxOutputStream(Project mainProject, boolean isErrorStream) {
		this.isErrorStream = isErrorStream;
		this.mainThreadGroup = resolveThreadGroup();
		this.mainDemuxOs = newDemuxOs(mainProject);

		this.streams.put(mainThreadGroup, mainDemuxOs);
	}

	public void bindProjectToCurrentThread(Project subProject) {
		streams.put(resolveThreadGroup(), newDemuxOs(subProject));
	}

	private DemuxOutputStream newDemuxOs(Project subProject) {
		return new DemuxOutputStream(subProject, isErrorStream);
	}

	public void bindMainProjectToCurrentThread() {
		this.streams.put(resolveThreadGroup(), mainDemuxOs);
	}

	public DemuxOutputStream getCurrentDemuxOutputStream() {
		return resolveDelegate();
	}
	
	// OutputStreamMethods

	@Override
	public void write(int b) throws IOException {
		resolveDelegate().write(b);
	}

	@Override
	public void write(byte[] b) throws IOException {
		resolveDelegate().write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		resolveDelegate().write(b, off, len);
	}

	@Override
	public void flush() throws IOException {
		resolveDelegate().flush();
	}

	@Override
	public void close() throws IOException {
		resolveDelegate().close();
	}

	private DemuxOutputStream resolveDelegate() {
		DemuxOutputStream result = streams.get(resolveThreadGroup());
		if (result == null)
			result = streams.get(mainThreadGroup);
		return result;
	}

	private ThreadGroup resolveThreadGroup() {
		return Thread.currentThread().getThreadGroup();
	}

}
