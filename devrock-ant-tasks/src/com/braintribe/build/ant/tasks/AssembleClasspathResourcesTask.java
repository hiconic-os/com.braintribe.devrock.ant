package com.braintribe.build.ant.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Materializes resources referenced by {@code META-INF/classpath-index.txt} in an artifact-scoped filesystem mirror.
 * Marked, resource-only artifacts can optionally be removed from the assembled application's library folder.
 */
public class AssembleClasspathResourcesTask extends Task {

	public static final String INDEX_PATH = "META-INF/classpath-index.txt";
	public static final String RESOURCE_ONLY_MARKER_PATH = "META-INF/classpath-resource-only";
	public static final String ORIGIN_PATH = "META-INF/classpath-origin.properties";
	public static final String MIRROR_FOLDER = "classpath-resources";

	private String classpathRefId;
	private File applicationDir;
	private boolean pruneResourceOnlyArtifacts;
	private String terminalArtifactId;

	public void setClasspathRefId(String classpathRefId) {
		this.classpathRefId = classpathRefId;
	}

	public void setApplicationDir(File applicationDir) {
		this.applicationDir = applicationDir;
	}

	public void setPruneResourceOnlyArtifacts(boolean pruneResourceOnlyArtifacts) {
		this.pruneResourceOnlyArtifacts = pruneResourceOnlyArtifacts;
	}

	public void setTerminalArtifactId(String terminalArtifactId) {
		this.terminalArtifactId = terminalArtifactId;
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

		Path mirrorRoot = applicationDir.toPath().resolve(MIRROR_FOLDER);
		recreateDirectory(mirrorRoot);

		List<String> classpath = new ArrayList<>(Arrays.asList(((org.apache.tools.ant.types.Path) reference).list()));
		classpath.sort(String::compareTo);

		List<ArtifactMirror> mirrors = new ArrayList<>();
		Set<String> artifactFolders = new LinkedHashSet<>();
		for (String element : classpath) {
			File classpathElement = new File(element);
			ArtifactMirror mirror = classpathElement.isDirectory()
					? mirrorDirectory(classpathElement, mirrorRoot, artifactFolders)
					: mirrorArchive(classpathElement, mirrorRoot, artifactFolders);
			if (mirror != null)
				mirrors.add(mirror);
		}

		writeInventory(mirrorRoot, mirrors);
		log("Materialized " + mirrors.size() + " classpath resource artifact(s) in " + mirrorRoot, Project.MSG_INFO);
	}

	private ArtifactMirror mirrorDirectory(File directory, Path mirrorRoot, Set<String> artifactFolders) {
		Path index = directory.toPath().resolve(INDEX_PATH);
		if (!Files.isRegularFile(index))
			return null;

		Path marker = directory.toPath().resolve(RESOURCE_ONLY_MARKER_PATH);
		boolean resourceOnly = Files.isRegularFile(marker);
		if (resourceOnly)
			validateResourceOnlyMarker(() -> Files.newInputStream(marker), marker.toString());
		String origin = terminalArtifactId != null && "build".equals(directory.getName()) ? terminalArtifactId : directory.getName();
		String folder = uniqueFolder(sanitize(origin), artifactFolders);
		Path artifactRoot = mirrorRoot.resolve(folder);
		List<String> entries = readIndex(() -> Files.newInputStream(index), index.toString());

		for (String entry : entries) {
			Path source = directory.toPath().resolve(entry).normalize();
			if (!source.startsWith(directory.toPath().normalize()) || !Files.isRegularFile(source))
				throw new BuildException("Indexed classpath resource does not exist in " + directory + ": " + entry);
			copy(source, safeTarget(artifactRoot, entry));
		}
		copy(index, artifactRoot.resolve(INDEX_PATH));
		writeOrigin(artifactRoot, origin, directory.getName());
		return new ArtifactMirror(origin, directory.getName(), folder, resourceOnly, false, entries);
	}

	private ArtifactMirror mirrorArchive(File archive, Path mirrorRoot, Set<String> artifactFolders) {
		if (!archive.isFile())
			return null;

		try (ZipFile zip = new ZipFile(archive)) {
			ZipEntry indexEntry = zip.getEntry(INDEX_PATH);
			if (indexEntry == null)
				return null;

			ZipEntry marker = zip.getEntry(RESOURCE_ONLY_MARKER_PATH);
			boolean resourceOnly = marker != null;
			if (resourceOnly)
				validateResourceOnlyMarker(() -> zip.getInputStream(marker), archive + "!/" + RESOURCE_ONLY_MARKER_PATH);
			String origin = inferArtifactId(archive);
			String folder = uniqueFolder(sanitize(stripJarSuffix(archive.getName())), artifactFolders);
			Path artifactRoot = mirrorRoot.resolve(folder);
			List<String> entries;
			try (InputStream in = zip.getInputStream(indexEntry)) {
				entries = readIndex(() -> in, archive + "!/" + INDEX_PATH);
			}

			for (String entry : entries) {
				ZipEntry resource = zip.getEntry(entry);
				if (resource == null || resource.isDirectory())
					throw new BuildException("Indexed classpath resource does not exist in " + archive + ": " + entry);
				Path target = safeTarget(artifactRoot, entry);
				try (InputStream in = zip.getInputStream(resource)) {
					copy(in, target);
				}
			}
			try (InputStream in = zip.getInputStream(indexEntry)) {
				copy(in, artifactRoot.resolve(INDEX_PATH));
			}
			writeOrigin(artifactRoot, origin, archive.getName());

			boolean pruned = resourceOnly && pruneResourceOnlyArtifacts;
			if (pruned)
				pruneLibraryArchive(archive);

			return new ArtifactMirror(origin, archive.getName(), folder, resourceOnly, pruned, entries);

		} catch (ZipException e) {
			log("Skipping non-archive runtime part " + archive, Project.MSG_VERBOSE);
			return null;
		} catch (IOException e) {
			throw new BuildException("Cannot inspect runtime artifact " + archive, e);
		}
	}

	private void pruneLibraryArchive(File sourceArchive) {
		Path target = applicationDir.toPath().resolve("lib").resolve(sourceArchive.getName());
		try {
			if (Files.deleteIfExists(target))
				log("Removed mirrored resource-only artifact from image library: " + target.getFileName(), Project.MSG_INFO);
			else
				throw new BuildException("Cannot prune resource-only artifact because it is not present in application/lib: " + target);
		} catch (IOException e) {
			throw new BuildException("Cannot prune resource-only artifact " + target, e);
		}
	}

	private List<String> readIndex(InputStreamSupplier input, String source) {
		Set<String> result = new LinkedHashSet<>();
		try (InputStream in = input.open();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			int lineNumber = 0;
			for (String line; (line = reader.readLine()) != null;) {
				lineNumber++;
				String entry = line.trim();
				if (entry.isEmpty() || entry.startsWith("#"))
					continue;
				if (entry.startsWith("/") || entry.contains("\\") || entry.contains("../") || entry.equals(".."))
					throw new BuildException("Unsafe classpath index entry at " + source + ":" + lineNumber + ": " + entry);
				result.add(entry);
			}
		} catch (IOException e) {
			throw new BuildException("Cannot read classpath index " + source, e);
		}
		return new ArrayList<>(result);
	}

	private void validateResourceOnlyMarker(InputStreamSupplier input, String source) {
		Properties properties = new Properties();
		try (InputStream in = input.open()) {
			properties.load(in);
		} catch (IOException e) {
			throw new BuildException("Cannot read classpath resource-only marker " + source, e);
		}
		if (!"1".equals(properties.getProperty("formatVersion")))
			throw new BuildException("Unsupported classpath resource-only marker in " + source + ". Expected formatVersion=1.");
	}

	private Path safeTarget(Path artifactRoot, String entry) {
		Path target = artifactRoot.resolve(entry).normalize();
		if (!target.startsWith(artifactRoot.normalize()))
			throw new BuildException("Classpath index entry escapes artifact mirror: " + entry);
		return target;
	}

	private void copy(Path source, Path target) {
		try {
			Files.createDirectories(target.getParent());
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new BuildException("Cannot copy " + source + " to " + target, e);
		}
	}

	private void copy(InputStream source, Path target) {
		try {
			Files.createDirectories(target.getParent());
			Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new BuildException("Cannot write mirrored classpath resource " + target, e);
		}
	}

	private void writeOrigin(Path artifactRoot, String artifactId, String sourceName) {
		String content = "artifactId=" + escapeProperty(artifactId) + "\nsourceName=" + escapeProperty(sourceName) + "\n";
		try {
			Path target = artifactRoot.resolve(ORIGIN_PATH);
			Files.createDirectories(target.getParent());
			Files.writeString(target, content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new BuildException("Cannot write classpath resource origin for " + artifactRoot, e);
		}
	}

	private void writeInventory(Path mirrorRoot, List<ArtifactMirror> mirrors) {
		mirrors.sort(Comparator.comparing(m -> m.folder));
		StringBuilder json = new StringBuilder("{\n  \"formatVersion\": 1,\n  \"artifacts\": [");
		for (int i = 0; i < mirrors.size(); i++) {
			ArtifactMirror mirror = mirrors.get(i);
			json.append(i == 0 ? "\n" : ",\n");
			json.append("    {\n");
			json.append("      \"artifactId\": \"").append(escapeJson(mirror.artifactId)).append("\",\n");
			json.append("      \"sourceName\": \"").append(escapeJson(mirror.sourceName)).append("\",\n");
			json.append("      \"folder\": \"").append(escapeJson(mirror.folder)).append("\",\n");
			json.append("      \"resourceOnly\": ").append(mirror.resourceOnly).append(",\n");
			json.append("      \"disposition\": \"").append(mirror.pruned ? "MIRRORED_ONLY" : "MIRRORED_AND_CLASSPATH").append("\",\n");
			json.append("      \"resources\": [");
			for (int e = 0; e < mirror.entries.size(); e++) {
				String entry = mirror.entries.get(e);
				Path resource = mirrorRoot.resolve(mirror.folder).resolve(entry);
				json.append(e == 0 ? "\n" : ",\n");
				json.append("        { \"path\": \"").append(escapeJson(entry)).append("\", \"sha256\": \"")
						.append(sha256(resource)).append("\" }");
			}
			if (!mirror.entries.isEmpty())
				json.append('\n');
			json.append("      ]\n");
			json.append("    }");
		}
		if (!mirrors.isEmpty())
			json.append('\n');
		json.append("  ]\n}\n");
		try {
			Files.writeString(mirrorRoot.resolve("index.json"), json, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new BuildException("Cannot write classpath resource inventory", e);
		}
	}

	private String sha256(Path path) {
		try (InputStream in = Files.newInputStream(path)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[8192];
			for (int read; (read = in.read(buffer)) >= 0;)
				digest.update(buffer, 0, read);
			StringBuilder result = new StringBuilder();
			for (byte value : digest.digest())
				result.append(String.format("%02x", value & 0xff));
			return result.toString();
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new BuildException("Cannot hash mirrored classpath resource " + path, e);
		}
	}

	private void recreateDirectory(Path directory) {
		if (Files.exists(directory)) {
			try (var paths = Files.walk(directory)) {
				paths.sorted(Comparator.reverseOrder()).forEach(path -> {
					try {
						Files.delete(path);
					} catch (IOException e) {
						throw new DeleteFailure(e);
					}
				});
			} catch (DeleteFailure e) {
				throw new BuildException("Cannot clean classpath resource mirror " + directory, e.getCause());
			} catch (IOException e) {
				throw new BuildException("Cannot inspect classpath resource mirror " + directory, e);
			}
		}
		try {
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new BuildException("Cannot create classpath resource mirror " + directory, e);
		}
	}

	private String inferArtifactId(File archive) {
		File versionDir = archive.getParentFile();
		File artifactDir = versionDir == null ? null : versionDir.getParentFile();
		if (versionDir != null && artifactDir != null) {
			String prefix = artifactDir.getName() + "-" + versionDir.getName();
			if (stripJarSuffix(archive.getName()).startsWith(prefix))
				return artifactDir.getName();
		}
		return stripJarSuffix(archive.getName());
	}

	private String uniqueFolder(String candidate, Set<String> folders) {
		if (!folders.add(candidate))
			throw new BuildException("Multiple indexed runtime artifacts map to the same mirror folder '" + candidate
					+ "'. Artifact file names must be unique in an RX application.");
		return candidate;
	}

	private String sanitize(String value) {
		String sanitized = value.replaceAll("[^A-Za-z0-9._-]", "_");
		return sanitized.isEmpty() ? "artifact" : sanitized;
	}

	private String stripJarSuffix(String name) {
		return name.endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
	}

	private String escapeProperty(String value) {
		return value.replace("\\", "\\\\").replace("\n", "\\n").replace("=", "\\=");
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
	}

	@FunctionalInterface
	private interface InputStreamSupplier {
		InputStream open() throws IOException;
	}

	private static class ArtifactMirror {
		final String artifactId;
		final String sourceName;
		final String folder;
		final boolean resourceOnly;
		final boolean pruned;
		final List<String> entries;

		ArtifactMirror(String artifactId, String sourceName, String folder, boolean resourceOnly, boolean pruned, List<String> entries) {
			this.artifactId = artifactId;
			this.sourceName = sourceName;
			this.folder = folder;
			this.resourceOnly = resourceOnly;
			this.pruned = pruned;
			this.entries = entries;
		}
	}

	private static class DeleteFailure extends RuntimeException {
		private static final long serialVersionUID = 1L;

		DeleteFailure(IOException cause) {
			super(cause);
		}
	}
}
