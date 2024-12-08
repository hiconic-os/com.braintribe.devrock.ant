// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.UnsupportedOperation;

public class ProcessExecution {
	
	private static class ProcessStreamReader extends Thread {
		private final InputStream in;
		private final StringBuilder buffer = new StringBuilder();
		private ProcessNotificationListener listener;
		
		public ProcessStreamReader(InputStream in) {
			this.in = in;
		}
			
		public void setListener(ProcessNotificationListener listener) {
			this.listener = listener;
		}

		@Override
		public void run() {
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(in, getConsoleEncoding()));
				String line = null;
				while ((line = reader.readLine()) != null) {
					if (listener != null) {
						listener.acknowledgeProcessNotification(MessageType.info, line);
					}
					if (buffer.length() > 0)
						buffer.append('\n');
					buffer.append(line);

				}
			} catch (IOException e) {
				// ignore
			}
		}
		
		public String getStreamResults() {
			return buffer.toString();
		}
		
		public void cancel() {
			if (isAlive()) {
				interrupt();
				try {
					join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// Copied from devrock.process.execution.ProcessExecution in devrock-cicd-steps 
	public static Maybe<String> runCommand(File cwd, String ... cmd) {
		List<String> commands = Arrays.asList(cmd);
		ProcessResults results;
		
		try {
			results = runCommand(commands, cwd, null, (t, m) -> {
				// NO OP
			});

		} catch (ProcessException e) {
			String command = commands.stream().collect(Collectors.joining(" "));
			return Reasons.build(UnsupportedOperation.T).text("Command [" + command + "] is not supported: " + e.getMessage()).toMaybe();
		}
		
		if (results.getRetVal() != 0) {
			String command = commands.stream().collect(Collectors.joining(" "));
			return Reasons.build(Reason.T) //
					.text("Command [" + command + "] execution in directory [" + cwd.getAbsolutePath() + "] failed with error code ["
							+ results.getRetVal() + "]: " + results.getErrorText()) //
					.toMaybe();
		}
		
		return Maybe.complete(results.getNormalText());
	}

	public static ProcessResults runCommand( ProcessNotificationListener listener, Map<String, String> environment, String ... cmd) throws ProcessException {
		return runCommand(Arrays.asList( cmd), null, environment, listener);
	}
	
	public static ProcessResults runCommand( ProcessNotificationListener listener, File workingCopy, Map<String, String> environment, String ... cmd) throws ProcessException {
		return runCommand(Arrays.asList( cmd), workingCopy, environment, listener);
	}

	public static ProcessResults runCommand(List<String> cmd, File workingDirectory, Map<String, String> environment, ProcessNotificationListener monitor) throws ProcessException {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder( cmd);
			
			if (workingDirectory != null) {
				processBuilder.directory( workingDirectory);
			}
			
			if (environment != null) {
				Map<String, String> processBuilderEnvironment = processBuilder.environment();
				processBuilderEnvironment.putAll(environment);
			}
			
			Process process = processBuilder.start();

			ProcessStreamReader errorReader = new ProcessStreamReader(process.getErrorStream());
			ProcessStreamReader inputReader = new ProcessStreamReader(process.getInputStream());
			
			inputReader.setListener(monitor);
			errorReader.setListener(monitor);
			
			errorReader.start();
			inputReader.start();
			
			process.waitFor();
			int retVal = process.exitValue();

			inputReader.cancel();
			errorReader.cancel();

			return new ProcessResults(retVal, inputReader.getStreamResults(), errorReader.getStreamResults());
		}
		catch (Exception e) {
			throw new ProcessException(e);
		}
	}

	public static String getConsoleEncoding() {
		return "Cp850";
	}

}
