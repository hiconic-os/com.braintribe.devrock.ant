package com.braintribe.devrock.ant.test.build;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.braintribe.build.ant.tasks.BuildApplicationImageTask;

public class BuildApplicationImageTaskTest {

	@Test
	public void generatesPortableApplicationDockerfile() {
		TestTask task = new TestTask();
		task.setBaseImage("ghcr.io/example/runtime-base:1.0");
		task.setExposedPorts("8080, 8443");
		task.setEnvironment("CUSTOMIZATION_NAME=proventem;CUSTOMIZATION_STAGE=local");

		assertThat(task.generatedDockerfile()).isEqualTo("""
				ARG BASE_IMAGE=ghcr.io/example/runtime-base:1.0
				FROM ${BASE_IMAGE}

				ENV CUSTOMIZATION_NAME="proventem"
				ENV CUSTOMIZATION_STAGE="local"

				COPY . /app
				WORKDIR /app
				EXPOSE 8080 8443
				CMD ["/app/bin/run"]
				""");
	}

	private static class TestTask extends BuildApplicationImageTask {
		String generatedDockerfile() {
			return dockerfile();
		}
	}
}
