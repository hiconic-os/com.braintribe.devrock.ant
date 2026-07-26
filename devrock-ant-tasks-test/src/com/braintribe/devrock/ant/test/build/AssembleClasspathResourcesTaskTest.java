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
		entries.put(AssembleClasspathResourcesTask.INDEX_PATH, "# raw index\nHICONIC-CONF/example.yaml\n");
		entries.put(AssembleClasspathResourcesTask.RESOURCE_ONLY_MARKER_PATH, "formatVersion=1\n");
		entries.put("HICONIC-CONF/example.yaml", "value: ${still-unresolved}\n");
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

		Path mirror = application.resolve("classpath-resources/example-configuration-1.0");
		assertThat(mirror.resolve("HICONIC-CONF/example.yaml")).hasContent("value: ${still-unresolved}");
		assertThat(mirror.resolve(AssembleClasspathResourcesTask.INDEX_PATH)).hasContent("# raw index\nHICONIC-CONF/example.yaml");
		assertThat(application.resolve("lib").resolve(source.getFileName())).doesNotExist();
		assertThat(application.resolve("classpath-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_ONLY\"")
				.contains("\"sha256\"");
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
		assertThat(application.resolve("classpath-resources/index.json")).content()
				.contains("\"disposition\": \"MIRRORED_AND_CLASSPATH\"");
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
