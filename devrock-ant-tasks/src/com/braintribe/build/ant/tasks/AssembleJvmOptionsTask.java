package com.braintribe.build.ant.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/** Collects additive {@code HICONIC-CONF/jvm.options} contributions into the assembled application's config folder. */
public class AssembleJvmOptionsTask extends Task {

	private static final String CONTRIBUTION_PATH = "HICONIC-CONF/jvm.options";

	private String classpathRefId;
	private File applicationDir;

	public void setClasspathRefId(String classpathRefId) {
		this.classpathRefId = classpathRefId;
	}

	public void setApplicationDir(File applicationDir) {
		this.applicationDir = applicationDir;
	}

	@Override
	public void execute() throws BuildException {
		if (classpathRefId == null)
			throw new BuildException("Attribute 'classpathRefId' must be set.");
		if (applicationDir == null)
			throw new BuildException("Attribute 'applicationDir' must be set.");

		Object reference = getProject().getReference(classpathRefId);
		if (!(reference instanceof org.apache.tools.ant.types.Path))
			throw new BuildException("Reference '" + classpathRefId + "' is not an Ant Path.");

		List<String> classpath = new ArrayList<>(Arrays.asList(((org.apache.tools.ant.types.Path) reference).list()));
		classpath.sort(String::compareTo);
		Set<String> options = new LinkedHashSet<>();
		for (String element : classpath)
			readOptions(new File(element), options);

		if (options.isEmpty()) {
			log("No packaged JVM option contributions found.", Project.MSG_VERBOSE);
			return;
		}

		Path target = applicationDir.toPath().resolve("conf/jvm.options");
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, options, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new BuildException("Cannot write assembled JVM options to " + target, e);
		}
	}

	private void readOptions(File classpathElement, Set<String> options) {
		if (classpathElement.isDirectory()) {
			File contribution = new File(classpathElement, CONTRIBUTION_PATH);
			if (contribution.isFile()) {
				try (InputStream in = Files.newInputStream(contribution.toPath())) {
					readOptionStream(in, options);
				} catch (IOException e) {
					throw new BuildException("Cannot read " + contribution, e);
				}
			}
			return;
		}

		if (!classpathElement.isFile())
			return;
		try (ZipFile zip = new ZipFile(classpathElement)) {
			ZipEntry entry = zip.getEntry(CONTRIBUTION_PATH);
			if (entry != null) {
				try (InputStream in = zip.getInputStream(entry)) {
					readOptionStream(in, options);
				}
			}
		} catch (ZipException e) {
			log("Skipping non-archive runtime part " + classpathElement, Project.MSG_VERBOSE);
		} catch (IOException e) {
			throw new BuildException("Cannot inspect runtime artifact " + classpathElement, e);
		}
	}

	private void readOptionStream(InputStream in, Set<String> options) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			for (String line; (line = reader.readLine()) != null;) {
				String option = line.trim();
				if (!option.isEmpty() && !option.startsWith("#"))
					options.add(option);
			}
		}
	}
}
