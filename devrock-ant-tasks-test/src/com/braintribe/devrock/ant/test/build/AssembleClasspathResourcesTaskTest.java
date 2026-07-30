package com.braintribe.devrock.ant.test.build;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.braintribe.build.ant.tasks.AssembleClasspathResourcesTask;

public class AssembleClasspathResourcesTaskTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void mirrorsAndPrunesMarkedResourceArtifact() throws Exception {
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
		assertThat(application.resolve("lib").resolve(source.getFileName())).doesNotExist();
		assertThat(application.resolve("packaged-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_ONLY\"")
				.contains("\"sha256\"");
		assertThat(application.resolve("packaged-resources/index.properties")).content()
				.contains("formatVersion=1")
				.contains("artifact.0.origin=example-configuration")
				.contains("artifact.0.resource.0.path=HICONIC-CONF/example.yaml");
		assertThat(application.resolve("packaged-conf")).doesNotExist();
		assertThat(application.resolve("local/compose.yaml")).hasContent("services: {}");
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
		assertThat(application.resolve("packaged-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_AND_CLASSPATH\"");
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
