package com.braintribe.build.ant.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.resource.FileResource;

/**
 * Materializes web applications contributed by runtime dependencies.
 * <p>
 * Contributions are read from {@code HICONIC-CONF/webapp-dependencies.properties}. The intentionally simple,
 * line-oriented format is:
 *
 * <pre>
 * group:artifact#version/part=/server/path[;welcome=file]
 * </pre>
 *
 * The file is not interpreted as {@link java.util.Properties}; repeated lines remain visible and contributions from
 * all runtime artifacts are additive.
 */
public class AssembleWebAppsTask extends Task {

	private static final String CONTRIBUTION_PATH = "HICONIC-CONF/webapp-dependencies.properties";
	private static final String GENERATED_CONFIG = "static-web-server-configuration.packaged-webapps.yaml";

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

		Map<String, WebAppContribution> byServerPath = new LinkedHashMap<>();
		for (String element : classpath)
			readContributions(new File(element), byServerPath);

		if (byServerPath.isEmpty()) {
			log("No packaged web application contributions found.", Project.MSG_VERBOSE);
			return;
		}

		List<WebAppContribution> contributions = new ArrayList<>(byServerPath.values());
		contributions.sort(Comparator.comparing(c -> c.serverPath));

		McBridge bridge = Bridges.getInstance(getProject());
		for (WebAppContribution contribution : contributions)
			materialize(bridge, contribution);

		writeWebServerConfiguration(contributions);
	}

	private void readContributions(File classpathElement, Map<String, WebAppContribution> byServerPath) {
		if (classpathElement.isDirectory()) {
			File contribution = new File(classpathElement, CONTRIBUTION_PATH);
			if (contribution.isFile()) {
				try (InputStream in = Files.newInputStream(contribution.toPath())) {
					readContributionStream(in, contribution.toString(), byServerPath);
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
					readContributionStream(in, classpathElement + "!/" + CONTRIBUTION_PATH, byServerPath);
				}
			}
		} catch (ZipException e) {
			// A runtime path can contain non-archive parts. Such parts cannot carry a contribution.
			log("Skipping non-archive runtime part " + classpathElement, Project.MSG_VERBOSE);
		} catch (IOException e) {
			throw new BuildException("Cannot inspect runtime artifact " + classpathElement, e);
		}
	}

	private void readContributionStream(InputStream in, String source, Map<String, WebAppContribution> byServerPath) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			int lineNumber = 0;
			for (String line; (line = reader.readLine()) != null;) {
				lineNumber++;
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
					continue;

				WebAppContribution contribution = parse(trimmed, source + ":" + lineNumber);
				WebAppContribution previous = byServerPath.putIfAbsent(contribution.serverPath, contribution);
				if (previous != null && !previous.equals(contribution))
					throw new BuildException("Conflicting packaged web applications for path '" + contribution.serverPath + "': "
							+ previous.source + " and " + contribution.source);
			}
		}
	}

	private WebAppContribution parse(String line, String source) {
		int equals = line.indexOf('=');
		if (equals <= 0 || equals == line.length() - 1)
			throw new BuildException("Invalid web application contribution at " + source + ": " + line);

		String dependencyAndPart = line.substring(0, equals).trim();
		String mapping = line.substring(equals + 1).trim();

		String welcome = null;
		int option = mapping.indexOf(';');
		if (option >= 0) {
			String options = mapping.substring(option + 1).trim();
			mapping = mapping.substring(0, option).trim();
			for (String token : options.split(";")) {
				String value = token.trim();
				if (value.startsWith("welcome="))
					welcome = nonEmpty(value.substring("welcome=".length()), "welcome file", source);
				else if (!value.isEmpty())
					throw new BuildException("Unknown web application option '" + value + "' at " + source);
			}
		}

		String serverPath = normalizeServerPath(mapping, source);
		int partSeparator = dependencyAndPart.lastIndexOf('/');
		if (partSeparator <= 0 || partSeparator == dependencyAndPart.length() - 1)
			throw new BuildException("Expected dependency/part at " + source + ": " + dependencyAndPart);

		String dependency = dependencyAndPart.substring(0, partSeparator);
		String part = dependencyAndPart.substring(partSeparator + 1);
		try {
			CompiledDependencyIdentification.parse(dependency);
			PartIdentification.parse(part);
		} catch (RuntimeException e) {
			throw new BuildException("Invalid dependency or part at " + source + ": " + dependencyAndPart, e);
		}

		return new WebAppContribution(dependency, part, serverPath, welcome, source);
	}

	private void materialize(McBridge bridge, WebAppContribution contribution) {
		CompiledDependencyIdentification dependency = CompiledDependencyIdentification.parse(contribution.dependency);
		PartIdentification partIdentification = PartIdentification.parse(contribution.part);
		final Artifact artifact;
		try {
			artifact = bridge.resolveArtifact(dependency, partIdentification);
		} catch (Exception e) {
			throw bridge.produceContextualizedBuildException("Cannot resolve packaged web application " + contribution.dependency
					+ "/" + contribution.part, e);
		}

		if (artifact.hasFailed())
			throw bridge.produceContextualizedBuildException(artifact.getFailure().stringify());

		Part part = artifact.getParts().get(partIdentification.asString());
		if (part == null || !(part.getResource() instanceof FileResource))
			throw new BuildException("Resolved web application has no file-backed part " + partIdentification.asString() + ": "
					+ contribution.dependency);

		File archive = new File(((FileResource) part.getResource()).getPath());
		Path target = applicationDir.toPath().resolve("web-app").resolve(stripLeadingSlash(contribution.serverPath)).normalize();
		Path webAppRoot = applicationDir.toPath().resolve("web-app").normalize();
		if (!target.startsWith(webAppRoot))
			throw new BuildException("Illegal packaged web application target: " + contribution.serverPath);

		log("Unpacking " + contribution.dependency + "/" + contribution.part + " to " + target, Project.MSG_INFO);
		unzip(archive.toPath(), target);
	}

	private void unzip(Path archive, Path target) {
		try {
			Files.createDirectories(target);
			try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
				for (ZipEntry entry; (entry = zip.getNextEntry()) != null;) {
					Path destination = target.resolve(entry.getName()).normalize();
					if (!destination.startsWith(target))
						throw new BuildException("Archive entry escapes target directory: " + entry.getName());
					if (entry.isDirectory()) {
						Files.createDirectories(destination);
					} else {
						Files.createDirectories(destination.getParent());
						Files.copy(zip, destination, StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		} catch (IOException e) {
			throw new BuildException("Cannot unpack " + archive + " to " + target, e);
		}
	}

	private void writeWebServerConfiguration(List<WebAppContribution> contributions) {
		Path confDir = applicationDir.toPath().resolve("conf");
		Path config = confDir.resolve(GENERATED_CONFIG);
		try {
			Files.createDirectories(confDir);
			try (OutputStream out = Files.newOutputStream(config)) {
				StringBuilder yaml = new StringBuilder("resourceMappings:\n");
				for (WebAppContribution contribution : contributions) {
					yaml.append("  - path: ").append(yamlString(stripLeadingSlash(contribution.serverPath))).append('\n');
					yaml.append("    rootDir: ").append(yamlString("${reflex.app.dir}/web-app/" + stripLeadingSlash(contribution.serverPath))).append('\n');
					if (contribution.welcome != null) {
						yaml.append("    welcomeFiles:\n");
						yaml.append("      - ").append(yamlString(contribution.welcome)).append('\n');
					}
				}
				out.write(yaml.toString().getBytes(StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			throw new BuildException("Cannot write generated web server configuration " + config, e);
		}
	}

	private static String normalizeServerPath(String path, String source) {
		String value = nonEmpty(path, "server path", source).replace('\\', '/');
		if (!value.startsWith("/"))
			value = "/" + value;
		while (value.endsWith("/") && value.length() > 1)
			value = value.substring(0, value.length() - 1);
		if (value.equals("/") || value.contains("/../") || value.endsWith("/..") || value.contains("/./"))
			throw new BuildException("Illegal server path at " + source + ": " + path);
		return value;
	}

	private static String nonEmpty(String value, String label, String source) {
		String result = value.trim();
		if (result.isEmpty())
			throw new BuildException("Empty " + label + " at " + source);
		return result;
	}

	private static String stripLeadingSlash(String path) {
		return path.startsWith("/") ? path.substring(1) : path;
	}

	private static String yamlString(String value) {
		return "'" + value.replace("'", "''") + "'";
	}

	private static class WebAppContribution {
		private final String dependency;
		private final String part;
		private final String serverPath;
		private final String welcome;
		private final String source;

		private WebAppContribution(String dependency, String part, String serverPath, String welcome, String source) {
			this.dependency = dependency;
			this.part = part;
			this.serverPath = serverPath;
			this.welcome = welcome;
			this.source = source;
		}

		@Override
		public int hashCode() {
			return Objects.hash(dependency, part, serverPath, welcome);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof WebAppContribution))
				return false;
			WebAppContribution other = (WebAppContribution) obj;
			return Objects.equals(dependency, other.dependency) && Objects.equals(part, other.part)
					&& Objects.equals(serverPath, other.serverPath) && Objects.equals(welcome, other.welcome);
		}
	}
}
