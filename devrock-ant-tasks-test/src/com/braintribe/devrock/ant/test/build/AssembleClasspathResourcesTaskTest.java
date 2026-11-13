package com.braintribe.devrock.ant.test.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.build.ant.tasks.AssembleClasspathResourcesTask;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.resource.FileResource;

public class AssembleClasspathResourcesTaskTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void mirrorsAndReducesMarkedResourceArtifact() throws Exception {
		Path root = temporaryFolder.getRoot().toPath();
		Path source = root.resolve("example-configuration-1.0.jar");
		Map<String, String> entries = new LinkedHashMap<>();
		entries.put(AssembleClasspathResourcesTask.RESOURCE_ONLY_MARKER_PATH, "formatVersion=1\n");
		entries.put("HICONIC-CONF/example.yaml", "value: ${still-unresolved}\n");
		entries.put("HICONIC-RESOURCES/example.txt", "packaged resource\n");
		entries.put(AssembleClasspathResourcesTask.APPLICATION_RESOURCES_PREFIX + "local/compose.yaml", "services: {}\n");
		entries.put(AssembleClasspathResourcesTask.INDEX_PATH,
				"# raw index\nHICONIC-CONF/example.yaml\nHICONIC-RESOURCES/example.txt\n"
						+ AssembleClasspathResourcesTask.APPLICATION_RESOURCES_PREFIX + "local/compose.yaml\n");
		writeZip(source, entries);

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		Project project = new Project();
		project.init();
		org.apache.tools.ant.types.Path classpath = new org.apache.tools.ant.types.Path(project);
		classpath.setLocation(source.toFile());
		project.addReference("test.classpath", classpath);

		AssembleClasspathResourcesTask task = new AssembleClasspathResourcesTask();
		task.setProject(project);
		task.setClasspathRefId("test.classpath");
		task.setApplicationDir(application.toFile());
		task.setPruneResourceOnlyArtifacts(true);
		task.execute();

		Path mirror = application.resolve("packaged-resources/example-configuration-1.0");
		assertThat(mirror.resolve("HICONIC-CONF/example.yaml")).hasContent("value: ${still-unresolved}");
		assertThat(mirror.resolve("HICONIC-RESOURCES/example.txt")).hasContent("packaged resource");
		assertThat(mirror.resolve("META-INF")).doesNotExist();
		Path libraryJar = application.resolve("lib").resolve(source.getFileName());
		assertThat(libraryJar).exists();
		try (ZipFile zip = new ZipFile(libraryJar.toFile())) {
			assertThat(zip.getEntry("HICONIC-CONF/example.yaml")).isNull();
			assertThat(zip.getEntry(AssembleClasspathResourcesTask.INDEX_PATH)).isNull();
		}
		assertThat(application.resolve("packaged-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_AND_CLASSPATH\"")
				.contains("\"sha256\"");
		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("formatVersion=1")
				.contains("artifact.0.artifactId=example-configuration")
				.contains("artifact.0.resource.0.path=HICONIC-CONF/example.yaml");
		assertThat(application.resolve("packaged-conf")).doesNotExist();
		assertThat(application.resolve("local/compose.yaml")).hasContent("services: {}");
	}

	@Test
	public void retainsDescriptorOnlySetupJarAfterMirroring() throws Exception {
		Path root = temporaryFolder.newFolder("descriptor-only-setup").toPath();
		Path source = root.resolve("generali-dev-setup-1.0.37.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.RESOURCE_ONLY_MARKER_PATH, "formatVersion=1\n",
				AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH,
						"groupId=proventem.claims.generali\nartifactId=generali-dev-setup\nversion=1.0.37\n",
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: setup\n"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Path libraryJar = application.resolve("lib").resolve(source.getFileName());
		Files.copy(source, libraryJar);

		taskFor(source, application).execute();

		assertThat(libraryJar).exists();
		try (ZipFile zip = new ZipFile(libraryJar.toFile())) {
			assertThat(zip.getEntry(AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH)).isNotNull();
			assertThat(zip.getEntry(AssembleClasspathResourcesTask.INDEX_PATH)).isNull();
			assertThat(zip.getEntry("HICONIC-CONF/example.yaml")).isNull();
		}
		assertThat(application.resolve("packaged-resources/generali-dev-setup-1.0.37/HICONIC-CONF/example.yaml"))
				.hasContent("value: setup\n");
	}

	@Test
	public void keepsUnmarkedIndexedArtifactOnClasspath() throws Exception {
		Path root = temporaryFolder.newFolder("mixed").toPath();
		Path source = root.resolve("mixed-module-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: mixed\n",
				"example/Module.class", "not-a-real-class"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		Project project = new Project();
		project.init();
		org.apache.tools.ant.types.Path classpath = new org.apache.tools.ant.types.Path(project);
		classpath.setLocation(source.toFile());
		project.addReference("test.classpath", classpath);

		AssembleClasspathResourcesTask task = new AssembleClasspathResourcesTask();
		task.setProject(project);
		task.setClasspathRefId("test.classpath");
		task.setApplicationDir(application.toFile());
		task.setPruneResourceOnlyArtifacts(true);
		task.execute();

		assertThat(application.resolve("lib").resolve(source.getFileName())).exists();
		assertStrippedJar(application.resolve("lib").resolve(source.getFileName()), "example/Module.class", null);
		assertOriginalJar(source);
		assertThat(application.resolve("packaged-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_AND_CLASSPATH\"");
	}

	@Test
	public void keepsMarkedResourceArtifactWithGeneratedReflectionClass() throws Exception {
		Path root = temporaryFolder.newFolder("marked-with-reflection").toPath();
		Path source = root.resolve("example-configuration-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.RESOURCE_ONLY_MARKER_PATH, "formatVersion=1\n",
				AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH,
						"groupId=example\nartifactId=configuration\nversion=1.0\nreflection-class=example.Configuration\n",
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: example\n",
				"example/Configuration.class", "generated reflection bytecode"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Path libraryJar = application.resolve("lib").resolve(source.getFileName());
		Files.copy(source, libraryJar);

		AssembleClasspathResourcesTask task = taskFor(source, application);
		task.setPruneResourceOnlyArtifacts(true);
		task.execute();

		assertThat(libraryJar).exists();
		assertStrippedJar(libraryJar, "example/Configuration.class", AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH);
		assertOriginalJar(source);
		assertThat(application.resolve("packaged-resources/example-configuration-1.0/HICONIC-CONF/example.yaml"))
				.hasContent("value: example\n");
		assertThat(application.resolve("packaged-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_AND_CLASSPATH\"");
	}

	private static void assertStrippedJar(Path jar, String expectedClass, String expectedDescriptor) throws IOException {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			assertThat(zip.getEntry(expectedClass)).isNotNull();
			if (expectedDescriptor != null)
				assertThat(zip.getEntry(expectedDescriptor)).isNotNull();
			assertThat(zip.getEntry("HICONIC-CONF/example.yaml")).isNull();
			assertThat(zip.getEntry(AssembleClasspathResourcesTask.INDEX_PATH)).isNull();
			assertThat(zip.getEntry(AssembleClasspathResourcesTask.RESOURCE_ONLY_MARKER_PATH)).isNull();
		}
	}

	private static void assertOriginalJar(Path jar) throws IOException {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			assertThat(zip.getEntry("HICONIC-CONF/example.yaml")).isNotNull();
			assertThat(zip.getEntry(AssembleClasspathResourcesTask.INDEX_PATH)).isNotNull();
		}
	}

	@Test
	public void leavesSignedArchiveIntact() throws Exception {
		Path root = temporaryFolder.newFolder("signed").toPath();
		Path source = root.resolve("signed-module-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: signed\n",
				"META-INF/SIGNER.SF", "signature metadata",
				"example/Module.class", "bytecode"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Path libraryJar = application.resolve("lib").resolve(source.getFileName());
		Files.copy(source, libraryJar);

		taskFor(source, application).execute();

		assertThat(Files.readAllBytes(libraryJar)).isEqualTo(Files.readAllBytes(source));
		assertThat(application.resolve("packaged-resources/signed-module-1.0/HICONIC-CONF/example.yaml"))
				.hasContent("value: signed\n");
	}

	@Test
	public void keepsCopiedArchiveIntactWhenPruningIsDisabled() throws Exception {
		Path root = temporaryFolder.newFolder("pruning-disabled").toPath();
		Path source = root.resolve("module-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: unchanged\n",
				"example/Module.class", "bytecode"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Path libraryJar = application.resolve("lib").resolve(source.getFileName());
		Files.copy(source, libraryJar);

		AssembleClasspathResourcesTask task = taskFor(source, application);
		task.setPruneResourceOnlyArtifacts(false);
		task.execute();

		assertThat(Files.readAllBytes(libraryJar)).isEqualTo(Files.readAllBytes(source));
	}

	@Test
	public void canonicalizesLegacyWindowsIndexEntries() throws Exception {
		Path root = temporaryFolder.newFolder("windows-index").toPath();
		Path source = root.resolve("windows-configuration-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.RESOURCE_ONLY_MARKER_PATH, "formatVersion=1\n",
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF\\example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: windows\n"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		AssembleClasspathResourcesTask task = taskFor(source, application);
		task.execute();

		assertThat(application.resolve("packaged-resources/windows-configuration-1.0/HICONIC-CONF/example.yaml"))
				.hasContent("value: windows");
	}

	@Test(expected = BuildException.class)
	public void rejectsTraversalUsingLegacyWindowsSeparators() throws Exception {
		Path root = temporaryFolder.newFolder("unsafe-windows-index").toPath();
		Path source = root.resolve("unsafe-configuration-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "..\\outside.yaml\n",
				"outside.yaml", "unsafe\n"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		taskFor(source, application).execute();
	}

	@Test
	public void takesArtifactIdFromArtifactDescriptor() throws Exception {
		Path root = temporaryFolder.newFolder("descriptor").toPath();
		Path source = root.resolve("renamed-1.0-pc.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH, "groupId=example\nartifactId=described-configuration\nversion=1.0-pc\n",
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: described\n"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		taskFor(source, application).execute();

		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("artifact.0.artifactId=described-configuration");
	}

	@Test
	public void takesGroupIdFromArtifactDescriptor() throws Exception {
		Path root = temporaryFolder.newFolder("descriptor-group").toPath();
		Path source = root.resolve("renamed-1.0-pc.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH, "groupId=example\nartifactId=described-configuration\nversion=1.0-pc\n",
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: described\n"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		taskFor(source, application).execute();

		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("artifact.0.groupId=example");
	}

	/** Without a descriptor the groupId is not known, and the index says nothing rather than something wrong. */
	@Test
	public void omitsGroupIdWithoutArtifactDescriptor() throws Exception {
		Path root = temporaryFolder.newFolder("no-descriptor").toPath();
		Path source = root.resolve("plain-configuration-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml\n",
				"HICONIC-CONF/example.yaml", "value: plain\n"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		taskFor(source, application).execute();

		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("artifact.0.artifactId=")
				.doesNotContain("groupId");
	}

	/** An artifact built without artifact reflection has no descriptor, but the dependency resolution knows its coordinates. */
	@Test
	public void takesGroupIdAndArtifactIdFromResolutionWithoutArtifactDescriptor() throws Exception {
		Path root = temporaryFolder.newFolder("resolved").toPath();
		Path source = root.resolve("flat-name.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml",
				"HICONIC-CONF/example.yaml", "value: resolved"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		AssembleClasspathResourcesTask task = taskFor(source, application);
		task.getProject().addReference("test.resolution", resolution(source, "example.group", "resolved-configuration"));
		task.setResolutionId("test.resolution");
		task.execute();

		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("artifact.0.groupId=example.group")
				.contains("artifact.0.artifactId=resolved-configuration");
	}

	/** The descriptor is what the artifact says about itself, so it wins over the resolution. */
	@Test
	public void prefersArtifactDescriptorOverResolution() throws Exception {
		Path root = temporaryFolder.newFolder("described-and-resolved").toPath();
		Path source = root.resolve("described-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.ARTIFACT_DESCRIPTOR_PATH, "groupId=described.group\nartifactId=described-configuration",
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml",
				"HICONIC-CONF/example.yaml", "value: described"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));
		Files.copy(source, application.resolve("lib").resolve(source.getFileName()));

		AssembleClasspathResourcesTask task = taskFor(source, application);
		task.getProject().addReference("test.resolution", resolution(source, "resolved.group", "resolved-configuration"));
		task.setResolutionId("test.resolution");
		task.execute();

		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("artifact.0.groupId=described.group")
				.contains("artifact.0.artifactId=described-configuration");
	}

	/** The build folder of the terminal artifact is not part of its own resolution and carries no descriptor. */
	@Test
	public void takesGroupIdOfTerminalBuildFolderFromAttribute() throws Exception {
		Path root = temporaryFolder.newFolder("terminal").toPath();
		Path build = root.resolve("build");
		Files.createDirectories(build.resolve("META-INF"));
		Files.createDirectories(build.resolve("HICONIC-CONF"));
		Files.writeString(build.resolve(AssembleClasspathResourcesTask.INDEX_PATH), "HICONIC-CONF/example.yaml", StandardCharsets.UTF_8);
		Files.writeString(build.resolve("HICONIC-CONF/example.yaml"), "value: terminal", StandardCharsets.UTF_8);

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));

		AssembleClasspathResourcesTask task = taskFor(build, application);
		task.setTerminalGroupId("terminal.group");
		task.setTerminalArtifactId("terminal-app");
		task.execute();

		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("artifact.0.groupId=terminal.group")
				.contains("artifact.0.artifactId=terminal-app");
	}

	@Test(expected = BuildException.class)
	public void rejectsResolutionIdThatNamesNoResolution() throws Exception {
		Path root = temporaryFolder.newFolder("wrong-resolution").toPath();
		Path source = root.resolve("example-1.0.jar");
		writeZip(source, Map.of(
				AssembleClasspathResourcesTask.INDEX_PATH, "HICONIC-CONF/example.yaml",
				"HICONIC-CONF/example.yaml", "value: example"));

		Path application = root.resolve("application");
		Files.createDirectories(application.resolve("lib"));

		AssembleClasspathResourcesTask task = taskFor(source, application);
		task.setResolutionId("missing.resolution");
		task.execute();
	}

	/** A resolution with one solution, whose jar part is the given file. */
	private static AnalysisArtifactResolution resolution(Path jar, String groupId, String artifactId) {
		FileResource resource = FileResource.T.create();
		resource.setPath(jar.toAbsolutePath().toString());

		Part part = Part.T.create();
		part.setResource(resource);

		AnalysisArtifact solution = AnalysisArtifact.T.create();
		solution.setGroupId(groupId);
		solution.setArtifactId(artifactId);
		solution.setVersion("1.0");
		solution.getParts().put("jar", part);

		AnalysisArtifactResolution result = AnalysisArtifactResolution.T.create();
		result.getSolutions().add(solution);
		return result;
	}

	private AssembleClasspathResourcesTask taskFor(Path source, Path application) {
		Project project = new Project();
		project.init();
		org.apache.tools.ant.types.Path classpath = new org.apache.tools.ant.types.Path(project);
		classpath.setLocation(source.toFile());
		project.addReference("test.classpath", classpath);

		AssembleClasspathResourcesTask task = new AssembleClasspathResourcesTask();
		task.setProject(project);
		task.setClasspathRefId("test.classpath");
		task.setApplicationDir(application.toFile());
		task.setPruneResourceOnlyArtifacts(true);
		return task;
	}

	private void writeZip(Path target, Map<String, String> entries) throws IOException {
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(target))) {
			for (Map.Entry<String, String> entry : entries.entrySet()) {
				out.putNextEntry(new ZipEntry(entry.getKey()));
				out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
	}
}
