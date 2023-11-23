// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.ProviderException;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.codec.marshaller.stax.StaxMarshaller;
import com.braintribe.model.artifact.Solution;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;

public class SolutionExportTask extends Task {

	private File solutionsSourceFile;
	private File solutionsTargetFile;
	private File solutionsHashFile;

	public File getSolutionsSourceFile() {
		return this.solutionsSourceFile;
	}

	public void setSolutionsSourceFile(final File solutionsSourceFile) {
		this.solutionsSourceFile = solutionsSourceFile;
	}

	public File getSolutionsTargetFile() {
		return solutionsTargetFile;
	}

	public void setSolutionsTargetFile(File solutionsTargetFile) {
		this.solutionsTargetFile = solutionsTargetFile;
	}

	public File getSolutionsHashFile() {
		return this.solutionsSourceFile;
	}

	public void setSolutionsHashFile(final File solutionsHashFile) {
		this.solutionsHashFile = solutionsHashFile;
	}

	@Override
	public void execute() throws BuildException {
		String solutionsAsString = extractSolutions(this.solutionsSourceFile);
		if (solutionsTargetFile != null) {
			FileTools.writeStringToFile(solutionsTargetFile, solutionsAsString);
		}

		try {
			if (solutionsHashFile != null) {
				byte[] md5CheckSum = StringTools.getMD5CheckSum(solutionsAsString, "UTF-8");
				FileTools.writeStringToFile(solutionsHashFile, StringTools.toHex(md5CheckSum));
			}
		} catch (IOException e) {
			throw new BuildException("Failed while calculating md5 for:\n" + solutionsAsString, e);
		}
	}

	public static String extractSolutions(File solutionsFile) {
		Collection<Solution> solutions = null;

		try (InputStream in = new FileInputStream(solutionsFile)) {
			StaxMarshaller marshaller = new StaxMarshaller();
			solutions = (Collection<Solution>) marshaller.unmarshall(in);
		} catch (Exception e) {
			String msg = "cannot extract solutions from [" + solutionsFile.getAbsolutePath() + "]";
			throw new ProviderException(msg, e);
		}
		solutions = solutions.stream().sorted(Comparator.comparing(Solution::getArtifactId)).collect(Collectors.toList());
		
		String result = "";
		for (Solution solution : solutions) {
			String groupId = solution.getGroupId();
			String artifactName = solution.getArtifactId();
			String version = solution.getVersion().getOriginalVersionString();
			String fullQualifiedName = groupId + ":" + artifactName + "#" + version;
			result += fullQualifiedName + "\n";
		}

		return result;
	}
}
