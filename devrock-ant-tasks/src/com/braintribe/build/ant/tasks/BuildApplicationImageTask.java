package com.braintribe.build.ant.tasks;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

/**
 * Builds an OCI image directly from an assembled RX application directory.
 * <p>
 * The generated Dockerfile is streamed to BuildKit and therefore never becomes another source or
 * distribution artifact. The same task supports loading an image into the local Docker daemon and
 * pushing one or more pipeline tags.
 */
public class BuildApplicationImageTask extends Task {

	private File applicationDir;
	private String baseImage;
	private String imageNames;
	private String exposedPorts = "";
	private String environment = "";
	private String command = "/app/bin/run";
	private boolean push;
	private boolean pull;
	private boolean noCache;
	private String dockerExecutable = "docker";

	public void setApplicationDir(File applicationDir) {
		this.applicationDir = applicationDir;
	}

	public void setBaseImage(String baseImage) {
		this.baseImage = baseImage;
	}

	/** Comma-separated image coordinates. */
	public void setImageNames(String imageNames) {
		this.imageNames = imageNames;
	}

	/** Whitespace- or comma-separated container ports, e.g. {@code 8080 8443}. */
	public void setExposedPorts(String exposedPorts) {
		this.exposedPorts = exposedPorts;
	}

	/** Semicolon-separated {@code NAME=value} entries baked into the image. */
	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setPush(boolean push) {
		this.push = push;
	}

	public void setPull(boolean pull) {
		this.pull = pull;
	}

	public void setNoCache(boolean noCache) {
		this.noCache = noCache;
	}

	public void setDockerExecutable(String dockerExecutable) {
		this.dockerExecutable = dockerExecutable;
	}

	@Override
	public void execute() {
		if (applicationDir == null || !applicationDir.isDirectory())
			throw new BuildException("Assembled application directory does not exist: " + applicationDir);
		if (baseImage == null || baseImage.isBlank())
			throw new BuildException("Attribute 'baseImage' must be set.");

		List<String> images = split(imageNames, ",");
		if (images.isEmpty())
			throw new BuildException("Attribute 'imageNames' must contain at least one image coordinate.");

		List<String> args = new ArrayList<>();
		args.add(dockerExecutable);
		args.add("buildx");
		args.add("build");
		args.add("--file");
		args.add("-");
		args.add("--build-arg");
		args.add("BASE_IMAGE=" + baseImage);
		if (pull)
			args.add("--pull");
		if (noCache)
			args.add("--no-cache");
		args.add(push ? "--push" : "--load");
		for (String image : images) {
			args.add("--tag");
			args.add(image);
		}
		args.add(applicationDir.getAbsolutePath());

		String dockerfile = dockerfile();
		log("Building application image " + String.join(", ", images) + " from " + applicationDir, Project.MSG_INFO);
		log("Base image: " + baseImage + (push ? " (push)" : " (local load)"), Project.MSG_VERBOSE);

		ProcessBuilder processBuilder = new ProcessBuilder(args);
		processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
		try {
			Process process = processBuilder.start();
			try (OutputStream out = process.getOutputStream()) {
				out.write(dockerfile.getBytes(StandardCharsets.UTF_8));
			}
			int exitCode = process.waitFor();
			if (exitCode != 0)
				throw new BuildException("Docker Buildx failed with exit code " + exitCode + ".");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new BuildException("Interrupted while building application image.", e);
		} catch (IOException e) {
			throw new BuildException("Cannot execute Docker Buildx via '" + dockerExecutable + "'.", e);
		}
	}

	protected String dockerfile() {
		StringBuilder result = new StringBuilder();
		result.append("ARG BASE_IMAGE=").append(baseImage).append('\n');
		result.append("FROM ${BASE_IMAGE}\n\n");
		for (String entry : split(environment, ";")) {
			int equals = entry.indexOf('=');
			if (equals <= 0)
				throw new BuildException("Invalid image environment entry: " + entry);
			String name = entry.substring(0, equals).trim();
			String value = entry.substring(equals + 1);
			if (!name.matches("[A-Za-z_][A-Za-z0-9_]*"))
				throw new BuildException("Invalid image environment variable name: " + name);
			result.append("ENV ").append(name).append('=').append(dockerQuote(value)).append('\n');
		}
		if (!environment.isBlank())
			result.append('\n');
		result.append("COPY . /app\n");
		result.append("WORKDIR /app\n");
		List<String> ports = split(exposedPorts, "[,\\s]+");
		if (!ports.isEmpty())
			result.append("EXPOSE ").append(String.join(" ", ports)).append('\n');
		result.append("CMD [").append(jsonQuote(command)).append("]\n");
		return result.toString();
	}

	private List<String> split(String value, String separatorRegex) {
		if (value == null || value.isBlank())
			return List.of();
		return Arrays.stream(value.split(separatorRegex))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
	}

	private String dockerQuote(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}

	private String jsonQuote(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
	}
}
