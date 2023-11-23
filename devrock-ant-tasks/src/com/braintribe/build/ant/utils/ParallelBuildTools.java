// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.utils;

import com.braintribe.build.ant.tasks.TransitiveBuild;

/**
 * @author peter.gazdik
 */
public class ParallelBuildTools {

	/** This lock is in place in case we are running a parallel build - see {@link TransitiveBuild} */
	private static final String LOCAL_REPO_LOCK = new String("LocalRepoRelatedLock");

	/**
	 * Runs given {@link Runnable} using a static global lock, thus ensuring only a single task called via this method is being executed at a given
	 * time.
	 * <p>
	 * When implementing support for parallel builds I have encountered issues that broke my local repo, probably related to the fact that multiple
	 * threads were accessing it at the same time. This is an attempt to make sure that doesn't happen, by making sure local-repo related tasks are
	 * all called via this method.
	 * <p>
	 * More details - when building in parallel, multiple tasks might attempt to modify local repo at the same time. This might be multiple instances
	 * of the same task or even instances of different tasks. Either way, the fact that it's different instances means synchronizing their execute
	 * methods wouldn't help, because that is equivalent on locking on the task instance itself, so the different instances would still be able to run
	 * in parallel.
	 */
	public static void runGloballySynchronizedRepoRelatedTask(Runnable r) {
		synchronized (LOCAL_REPO_LOCK) {
			r.run();
		}
	}

}
