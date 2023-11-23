// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.utils.lcd.CollectionTools2.newConcurrentSet;
import static com.braintribe.utils.lcd.CollectionTools2.newList;
import static com.braintribe.utils.lcd.CollectionTools2.newTreeSet;
import static com.braintribe.utils.lcd.CollectionTools2.nullSafe;
import static com.braintribe.utils.lcd.StringTools.isEmpty;
import static java.util.Collections.emptySet;
import static org.apache.tools.ant.DrAnsiColorLogger.MSG_ARTIFACT;
import static org.apache.tools.ant.DrAnsiColorLogger.MSG_SPECIAL;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.tools.ant.DrAnsiColorLogger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Ant;
import org.apache.tools.ant.taskdefs.Property;

import com.braintribe.build.ant.listener.EventCollector;
import com.braintribe.build.ant.listener.EventCollector.EventEntry;
import com.braintribe.build.ant.utils.DrAntTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.devrock.mc.api.repository.CodebaseReflection;
import com.braintribe.execution.CountingThreadFactory;
import com.braintribe.execution.ExtendedThreadPoolExecutor;
import com.braintribe.execution.graph.api.ParallelGraphExecution;
import com.braintribe.execution.graph.api.ParallelGraphExecution.PgeItemResult;
import com.braintribe.execution.graph.api.ParallelGraphExecution.PgeResult;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.version.Version;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.encryption.Md5Tools;
import com.braintribe.utils.lcd.StringTools;

/**
 * A {@link ForBuildSet} implementation which performs configured {@link #setTarget(String) target} on every artifacts from given
 * {@link #setBuildSetRefId(String) buildSet}.
 * <p>
 * A parallel execution of this task is available, though the thread-safety depends on the thread-safety of the delegated target task. Parallel builds
 * are generally OK, as everything happens locally for each artifact, with the exception of the {@link InstallTask}
 * <p>
 * InstallTask has therefore been modified to only allow single threaded execution. The reason was that (old) malaclypse locks the entire local repo
 * and parallel execution can lead to <tt>java.lang.IllegalStateException: giving up trying to acquire lock on file [C:\.updateinfo.main]</tt>.
 */
public class TransitiveBuild extends ForBuildSet {

	public enum Mode {
		individual,
		shared
	}

	private String targetName = null;
	private File antfile = null;

	private Mode mode = Mode.shared;
	private Integer skip = null;
	private Integer numberOfThreads = null;
	private Boolean offline;
	private final List<String> artifactsToIgnore = newList();

	private final int DEFAULT_NUMBER_OF_THREADS_FOR_INSTALL = 6;

	@Configurable
	public void setOffline(Boolean offline) {
		this.offline = offline;
	}

	@Configurable
	public void setIgnore(String ignore) {
		String[] values = ignore.split("\\+");
		artifactsToIgnore.addAll(Arrays.asList(values));
	}

	private int getSkip() {
		if (skip == null)
			skip = getIntegerProperty("skip", 0);

		return skip;
	}

	private boolean getSkipParallel() {
		String skipValue = getProject().getProperty("skip");
		return "true".equalsIgnoreCase(skipValue);
	}

	/**
	 * Resolves number of threads based on <tt>-Dthreads=N</tt> parameter, if specified.
	 * <p>
	 * If not specified, the value is 1 except for "install" target, where we use {@link #DEFAULT_NUMBER_OF_THREADS_FOR_INSTALL}.
	 */
	private int getNumberOfThreads() {
		if (numberOfThreads == null) {
			numberOfThreads = getIntegerProperty("threads", -1);
			if (numberOfThreads == -1)
				numberOfThreads = "install".equals(targetName) ? DEFAULT_NUMBER_OF_THREADS_FOR_INSTALL : 1;
		}

		return numberOfThreads;
	}

	private Integer getIntegerProperty(String name, Integer defaultValue) {
		String encodedProperty = getProject().getProperty(name);
		if (isEmpty(encodedProperty))
			return defaultValue;

		try {
			return Integer.valueOf(encodedProperty);
		} catch (Exception e) {
			log("Unable to parse integer property [" + name + "], value [" + encodedProperty + "]. Using deafult: " + defaultValue, Project.MSG_WARN);
			return defaultValue;
		}
	}

	private boolean getOffline() {
		if (offline == null) {
			String encodedOffline = getProject().getProperty("offline");
			if (!isEmpty(encodedOffline))
				offline = Boolean.valueOf(encodedOffline);
			else
				offline = false;
		}
		return offline;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	private File getAntfile() {
		if (antfile == null) {
			switch (mode) {
				case shared:
					antfile = new File(getProject().getBaseDir(), "shared-build.xml");
					break;

				case individual:
				default:
					antfile = new File("build.xml");
					break;
			}
		}

		return antfile;
	}

	public void setAntfile(File antfile) {
		this.antfile = antfile;
	}

	public void setTarget(String target) {
		this.targetName = target;
	}

	@Override
	public void init() throws BuildException {
		super.init();
	}

	@Override
	protected void validate() {
		super.validate();

		if (targetName == null)
			throw new BuildException("you need to supply a target attribute");
	}

	@Override
	protected void process(CodebaseReflection codebaseReflection, AnalysisArtifactResolution resolution) {
		switch (targetName) {
			case "list-range":
				listRange(resolution);
				break;
			default:
				buildArtifacts(codebaseReflection, resolution);
				break;
		}
	}

	private void listRange(AnalysisArtifactResolution resolution) {
		StringBuilder outputFileContentBuilder = new StringBuilder();

		for (AnalysisArtifact solution : resolution.getSolutions()) {
			String name = solution.asString();

			if (shouldIgnore(solution)) {
				log("ignoring " + name);
				continue;
			}

			log(name);
			outputFileContentBuilder.append(name + "\n");
		}

		String outputFilePath = (String) PropertyHelper.getProperty(getProject(), "list-range-output-file");
		if (outputFilePath != null) {
			File outputFile = new File(outputFilePath);
			log("Writing solutions to " + outputFile.getAbsolutePath() + ".");
			FileTools.write(outputFile).string(outputFileContentBuilder.toString());
		}
	}

	protected void buildArtifacts(CodebaseReflection codebaseReflection, AnalysisArtifactResolution resolution) {
		String antfile = getAntfile().toString();
		log("executing " + antfile + " (target=" + targetName + ") for artifacts in order:");

		logSolutions(resolution);

		prepareBtDemuxOutputStream();
		try {
			if (isMultiThreaded())
				build_MultiThreaded(codebaseReflection, resolution, antfile);
			else
				build_SingleThreaded(codebaseReflection, resolution, antfile);
		} finally {
			cleanupDemuxOutputStream();
		}
	}

	private void logSolutions(AnalysisArtifactResolution resolution) {
		int i = 1;
		for (AnalysisArtifact artifact : resolution.getSolutions()) {
			String name = artifact.asString();

			if (artifactsToIgnore != null) {
				String identification = ArtifactIdentification.asString(artifact);
				if (artifactsToIgnore.contains(identification)) {
					log("ignoring " + name);
					continue;
				}
			}
			log(i++ + ": " + name);
		}
	}

	// ####################################################
	// ## . . . . . . . . Single-Threaded . . . . . . . .##
	// ####################################################

	private void build_SingleThreaded(CodebaseReflection codebaseReflection, AnalysisArtifactResolution resolution, String antfile) {
		BtAnt antTask = createAnt(antfile);
		int i = 1;

		BuildLogger logger = DrAntTools.findLogger(getProject());
		MsgLogger msgLogger = new MsgLogger(logger);

		List<AnalysisArtifact> solutions = resolution.getSolutions();
		for (AnalysisArtifact artifact : solutions) {
			String artifactName = artifact.asString();

			if (shouldIgnore(artifact)) {
				log("ignoring " + artifactName);
				continue;
			}

			File baseDir = findBaseDir(artifact, codebaseReflection);

			if (i <= getSkip()) {
				log("skipping artifact (" + i + "/" + solutions.size() + ") " + artifactName + " in " + baseDir);

			} else {
				logBuildingArtifact(msgLogger.log().text("\n"), artifact, i, 0, solutions, baseDir);

				antTask.setDir(baseDir);

				try {
					executeAndTime(antTask, artifactName, this::log);

				} catch (RuntimeException e) {
					throw handleSingleThreadException(e, i, msgLogger);
				}
			}
			i++;
		}
	}

	private boolean shouldIgnore(AnalysisArtifact artifact) {
		return artifactsToIgnore != null && //
				artifactsToIgnore.contains(ArtifactIdentification.asString(artifact));
	}

	private BuildException handleSingleThreadException(Exception e, int i, MsgLogger msgLogger) {
		if (i > 1)
			msgLogger.log() //
					.text("ERROR in transitive build. TO SKIP ALREADY BUILT ARTIFACTS YOU CAN USE '-Dskip=" + (i - 1)
							+ " ' PARAMETER after fixing the issue. This WORKS AS LONG AS YOU DON'T CHANGE DEPENDENCIES as part of your fix.")
					.please();

		return e instanceof BuildException ? (BuildException) e : new BuildException(e);
	}

	// ####################################################
	// ## . . . . . . . . Multi-Threaded . . . . . . . . ##
	// ####################################################

	private void build_MultiThreaded(CodebaseReflection codebaseReflection, AnalysisArtifactResolution resolution, String antfileName) {
		new MultiThreadedBuild(codebaseReflection, resolution, antfileName).pleaseBuildMySolutions();
	}

	class MultiThreadedBuild {

		private static final String TASK_NAME = "Transitive-Build";

		private final List<AnalysisArtifact> solutions;
		private final File alreadyBuiltTempFile;
		private final Set<String> alreadyBuiltNames;
		private final CodebaseReflection codebaseReflection;
		private final String antfileName;

		private final BuildLogger originalLogger;
		private final EventCollector eventCollector;
		private final MsgLogger msgLogger;

		private final AtomicInteger runningBuildsCounter = new AtomicInteger(0);
		private final AtomicInteger buildCounter = new AtomicInteger(0);

		public MultiThreadedBuild(CodebaseReflection codebaseReflection, AnalysisArtifactResolution resolution, String antfileName) {
			this.solutions = resolution.getSolutions();
			this.alreadyBuiltTempFile = getAlreadyBuiltSolutionNamesTmpFile();
			this.alreadyBuiltNames = newConcurrentSet(resolveSkippedSolutionNames());
			this.codebaseReflection = codebaseReflection;
			this.antfileName = antfileName;

			this.originalLogger = DrAntTools.findLogger(getProject());
			this.msgLogger = new MsgLogger(originalLogger);
			this.eventCollector = new EventCollector(originalLogger);
		}

		private Set<String> resolveSkippedSolutionNames() {
			if (!getSkipParallel())
				return emptySet();

			if (!alreadyBuiltTempFile.exists()) {
				log("Cannot use [skip] argument as the file with already built artifacts doesn't exist: " + alreadyBuiltTempFile.getPath(),
						Project.MSG_WARN);
				return emptySet();
			}

			return FileTools.read(alreadyBuiltTempFile) //
					.asLineStream() //
					.collect(Collectors.toSet());
		}

		private File getAlreadyBuiltSolutionNamesTmpFile() {
			File groupDir = getProject().getBaseDir();
			String groupDirPath = groupDir.getAbsolutePath();
			return FileTools.newTempFile("DevRock/ant/build-" + groupDir.getName() + "-" + Md5Tools.getMd5(groupDirPath));
		}

		public void pleaseBuildMySolutions() {
			logImmediately("\nBuilding with " + getNumberOfThreads() + " threads.\n", Project.MSG_INFO);
			switchListeners(originalLogger, eventCollector);

			try {
				PgeResult<AnalysisArtifact, Boolean> result = ParallelGraphExecution.foreach(TASK_NAME, solutions) //
						.itemsToProcessAfter(this::getDependers) //
						.withThreadPoolExecutor(newThreadPoolExecutor(getNumberOfThreads())) //
						.run(s -> buildOrSkipSingleArtifact(s));

				if (result.hasError()) {
					logFailedBuildsAsLast();
					logError(result);
					logImmediately("\nPARALLEL BUILD FINISHED WITH ERRORS!!!", Project.MSG_ERR);
					storeSkippedSolutions();
					throw new BuildException("Error in parallel build. See above for more details!");
				}

				alreadyBuiltTempFile.delete();
				logImmediately("Parallel build finished successfully!", Project.MSG_INFO);

			} finally {
				// TODO make sure all events are processed
				// Theoretically there could be events associated to ThreadGroups other than the main one and ones we create for each new thread.
				logCollectedEvents();
				switchListeners(eventCollector, originalLogger);
			}
		}

		private void storeSkippedSolutions() {
			TreeSet<String> skippedSorted = newTreeSet(alreadyBuiltNames);

			FileTools.write(alreadyBuiltTempFile).lines(skippedSorted);

		}

		private List<AnalysisArtifact> getDependers(AnalysisArtifact artifact) {
			List<AnalysisArtifact> dependers = new ArrayList<>(artifact.getDependers().size());
			for (AnalysisDependency dependency : artifact.getDependers()) {
				AnalysisArtifact depender = dependency.getDepender();
				if (depender != null) {
					dependers.add(depender);
				}
			}
			return dependers;
		}

		/** @see #groupPerThread_ThreadFactory() */
		private ThreadPoolExecutor newThreadPoolExecutor(int nThreads) {
			ExtendedThreadPoolExecutor threadPool = new ExtendedThreadPoolExecutor( //
					nThreads, nThreads, // core/max pool size
					0L, TimeUnit.MILLISECONDS, // keep alive time
					new LinkedBlockingQueue<>(), //
					groupPerThread_ThreadFactory());

			threadPool.setDescription("ParallelBuildExecutor");
			threadPool.postConstruct();

			return threadPool;
		}

		/**
		 * For every one of our threads that are processing the tasks in parallel we want to set it's own group - see
		 * {@link #newThreadWithItsOwnGroup(ThreadGroup, Runnable, String)}. There are some tasks that might create other threads, and if those other
		 * threads log something, we want to be able to recognize which tasks' thread it belongs to.
		 */
		private CountingThreadFactory groupPerThread_ThreadFactory() {
			CountingThreadFactory result = new CountingThreadFactory(TASK_NAME);
			result.setExtendedThreadFactory(this::newThreadWithItsOwnGroup);

			return result;
		}

		private Thread newThreadWithItsOwnGroup(ThreadGroup parentGroup, Runnable r, String name) {
			ThreadGroup group = new ThreadGroup(parentGroup, "Group-" + name);

			return new Thread(group, r, name, 0);
		}

		private void switchListeners(BuildListener toRemove, BuildListener toAdd) {
			Project project = getProject();
			project.removeBuildListener(toRemove);
			project.addBuildListener(toAdd);
		}

		private void logError(PgeResult<AnalysisArtifact, Boolean> result) {
			for (PgeItemResult<AnalysisArtifact, ?> pgeItemResult : result.itemResulsts().values()) {
				if (pgeItemResult.getError() != null) {
					log("Error while building: " + pgeItemResult.getItem().asString(), Project.MSG_ERR);
					log(pgeItemResult.getError(), Project.MSG_ERR);
					log("--------------", Project.MSG_ERR);
				}
			}

			msgLogger.log() //
					.text("\n\tAs this was a PARALLEL build, more than one build might have failed. ") //
					.text(Project.MSG_WARN, "BUT I DID MY BEST TO PRINT ALL THE FAILED BUILDS AT THE END :)\n") //
					.please();

			if (alreadyBuiltNames.isEmpty())
				return;

			msgLogger.log() //
					.text("\tTO SKIP ALREADY BUILT ARTIFACTS use '") //
					.text(MSG_SPECIAL, "-Dskip=true") //
					.text("' PARAMETER after fixing the issue.\n\t") //
					.text("The list of artifacts you've already built will be read from a temp file.") //
					.please();
		}

		private void buildOrSkipSingleArtifact(AnalysisArtifact artifact) {
			if (shouldIgnore(artifact))
				skipSingleArtifact(artifact, "IGNORING");
			else if (alreadyBuiltNames.contains(artifact.getArtifactId()))
				skipSingleArtifact(artifact, "SKIPPING");
			else
				buildSingleArtifact(artifact);

			alreadyBuiltNames.add(artifact.getArtifactId());
		}

		private void skipSingleArtifact(AnalysisArtifact artifact, String skipOrIgnoring) {
			File baseDir = findBaseDir(artifact, codebaseReflection);
			int buildNumber = buildCounter.incrementAndGet();

			String version = artifact.getVersion();

			msgLogger.log() //
					.text(MSG_SPECIAL, skipOrIgnoring + " build (" + buildNumber + "/" + solutions.size() + ") ") //
					.text(MSG_SPECIAL, artifact.getGroupId() + ":") //
					.text(MSG_ARTIFACT, artifact.getArtifactId()) //
					.text(MSG_SPECIAL, "#" + version) //
					.text(MSG_SPECIAL, " in " + baseDir) //
					.please(Project.MSG_INFO);
		}

		private final List<FailedBuild> failedBuilds = newList();

		private void buildSingleArtifact(AnalysisArtifact artifact) {
			try {
				BtAnt antTask = createAnt(antfileName);

				String artifactName = artifact.asString();

				File baseDir = findBaseDir(artifact, codebaseReflection);

				try {
					int buildNumber = buildCounter.incrementAndGet();
					int runningBuilds = runningBuildsCounter.incrementAndGet();

					logBuildingArtifact(msgLogger.log(), artifact, buildNumber, runningBuilds, solutions, baseDir);

					antTask.setDir(baseDir);
					logDelayedArtifactName(artifactName);
					executeAndTime(antTask, artifactName, this::logDelayed);

					logCollectedEvents();

				} finally {
					runningBuildsCounter.decrementAndGet();
				}

			} catch (BuildException e) {
				rememberFailedBuild(e);
				throw e;

			} catch (RuntimeException e) {
				e = new RuntimeException("Error while building " + antfileName, e);
				rememberFailedBuild(e);
				throw e;
			}
		}

		private void logDelayedArtifactName(String artifactName) {
			logDelayed("===================================================", Project.MSG_INFO);
			logDelayed("[ARTIFACT] " + artifactName, Project.MSG_INFO);
		}

		private void logDelayed(String message, int priority) {
			_log(eventCollector, message, priority);
		}

		private void logImmediately(String message, int priority) {
			_log(originalLogger, message, priority);
		}

		private void _log(BuildListener logger, String message, int priority) {
			final BuildEvent event = new BuildEvent(getProject());
			event.setMessage(message, priority);
			logger.messageLogged(event);
		}

		private synchronized void logCollectedEvents() {
			List<EventEntry> entries = eventCollector.removeEntries();
			logEvents(entries);
		}

		private void rememberFailedBuild(RuntimeException e) {
			failedBuilds.add(new FailedBuild(eventCollector.removeEntries(), e));
		}

		private void logFailedBuildsAsLast() {
			for (FailedBuild failedBuild : failedBuilds) {
				logEvents(failedBuild.eventEntries);

				if (!(failedBuild.e instanceof BuildException))
					failedBuild.e.printStackTrace();
			}
		}

		private void logEvents(List<EventEntry> entries) {
			for (EventEntry eventEntry : nullSafe(entries))
				eventEntry.applyOn(originalLogger);
			logImmediately("\n", Project.MSG_INFO);
		}

	}

	static class FailedBuild {
		public final List<EventEntry> eventEntries;
		public final RuntimeException e;

		public FailedBuild(List<EventEntry> eventEntries, RuntimeException e) {
			this.eventEntries = eventEntries;
			this.e = e;
		}
	}

	private void logBuildingArtifact(MsgLogger.MsgLogBuilder logger, AnalysisArtifact artifact, int buildNumber, int runningBuilds,
			List<AnalysisArtifact> solutions, File baseDir) {

		String version = artifact.getVersion();

		logger //
				.text(MSG_SPECIAL, "Starting build (" + buildNumber + "/" + solutions.size() + ") ") //
				.text(MSG_SPECIAL, runningBuildsInfoIfParallel(runningBuilds)) //
				.text(MSG_SPECIAL, artifact.getGroupId() + ":") //
				.text(MSG_ARTIFACT, artifact.getArtifactId()) //
				.text(MSG_SPECIAL, "#" + version) //
				.text(MSG_SPECIAL, " in " + baseDir) //
				.please();
	}

	private String runningBuildsInfoIfParallel(int runningBuilds) {
		return runningBuilds > 0 ? "(R:" + runningBuilds + ") " : "";
	}

	// ####################################################
	// ## . . . . . . . . Common helpers . . . . . . . . ##
	// ####################################################

	private File findBaseDir(AnalysisArtifact artifact, CodebaseReflection codebaseReflection) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		Version version = Version.parse(artifact.getVersion());
		Version majMin = Version.create(version.getMajor(), version.getMinor());
		String versionString = majMin.asString();

		return codebaseReflection.findArtifact(groupId, artifactId, versionString);
	}

	private BtAnt createAnt(String antfile) {
		BtAnt antTask = new BtAnt(this);
		antTask.init();
		antTask.setAntfile(antfile);
		antTask.setTarget(targetName);
		antTask.setInheritAll(false);

		// artifactRoots property = baseDir
		Property property = antTask.createProperty();
		property.setName("artifactsRoot");
		property.setLocation(getProject().getBaseDir());

		// offline
		Property offlineProperty = antTask.createProperty();
		offlineProperty.setName("offline");
		offlineProperty.setValue(String.valueOf(getOffline()));

		return antTask;
	}

	private boolean isMultiThreaded() {
		return getNumberOfThreads() > 1;
	}

	private void executeAndTime(BtAnt antTask, String artifactName, BiConsumer<String, Integer> log) {
		bindSubProjectToCurrentThread(antTask.getNewProject());

		try {
			long ms = System.currentTimeMillis();
			antTask.execute();
			ms = System.currentTimeMillis() - ms;
			log.accept("Building " + artifactName + " took " + StringTools.prettyPrintMilliseconds(ms, true), Project.MSG_INFO);

			ms = System.currentTimeMillis() - ms;
		} finally {
			unbindSubProjectFromCurrentThread();
		}
	}

	private class MsgLogger {

		private final BuildLogger logger;
		private final boolean color;

		public MsgLogger(BuildLogger logger) {
			this.logger = logger;
			this.color = logger instanceof DrAnsiColorLogger;
		}

		public MsgLogBuilder log() {
			return new MsgLogBuilder();
		}

		public class MsgLogBuilder {
			private final StringBuilder sb = new StringBuilder();

			public MsgLogBuilder text(String msg) {
				return text(Project.MSG_INFO, msg);
			}

			public MsgLogBuilder text(int priority, String msg) {
				sb.append(encode(msg, priority));
				return this;
			}

			public void please() {
				please(Project.MSG_INFO);
			}

			public void please(int priority) {
				if (color)
					((DrAnsiColorLogger) logger).printEncodedMessage(sb.toString());
				else {
					final BuildEvent event = new BuildEvent(getProject());
					event.setMessage(sb.toString(), priority);
					logger.messageLogged(event);
				}
			}
		}

		private String encode(String msg, int priority) {
			return color ? DrAnsiColorLogger.encode(msg, priority) : msg;
		}

	}

	// @formatter:off
	class BtAnt extends Ant {
		public BtAnt(Task owner) {  super(owner); }
		@Override public Project getNewProject() { return super.getNewProject(); }
	}
	// @formatter:on

	private DrDemuxOutputStream btDemuxOut, btDemuxErr;
	private PrintStream originalOut, originalErr;

	/** @see DrDemuxOutputStream */
	private void prepareBtDemuxOutputStream() {
		originalOut = System.out;
		originalErr = System.err;

		btDemuxOut = new DrDemuxOutputStream(getProject(), false);
		btDemuxErr = new DrDemuxOutputStream(getProject(), true);

		System.setOut(new PrintStream(btDemuxOut));
		System.setErr(new PrintStream(btDemuxErr));
	}

	private void cleanupDemuxOutputStream() {
		System.setOut(originalOut);
		System.setErr(originalErr);
	}

	private void bindSubProjectToCurrentThread(Project project) {
		btDemuxOut.bindProjectToCurrentThread(project);
		btDemuxErr.bindProjectToCurrentThread(project);
	}

	private void unbindSubProjectFromCurrentThread() {
		btDemuxOut.bindMainProjectToCurrentThread();
		btDemuxErr.bindMainProjectToCurrentThread();
	}
}
