// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.malaclypse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;

import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.stax.StaxMarshaller;
import com.braintribe.crypto.CryptoServiceException;
import com.braintribe.crypto.hash.md5.MD5HashGenerator;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.Solution;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.packaging.Packaging;
import com.braintribe.utils.DateTools;

/**
 * creates a list of special packaging artifacts used to show what the terminal artifact consists of
 * 
 * @author pit
 *
 */
public class ArtifactListProducer {
	public static enum HashType {
		revision,
		version
	}

	private static Logger log = Logger.getLogger(ArtifactListProducer.class);

	/**
	 * produces a file with the appropriate packaging information
	 * 
	 * @param pomFile
	 *            - the pom file
	 * @param terminalBuildArtifact
	 *            - the terminal {@link Artifact}
	 * @param solutions
	 *            - a {@link List} of {@link Solution} that contains all packaged parts
	 * @param outputFile
	 *            - the {@link File} to write to
	 * @param hashType
	 *            - what to use of the hash value, either the revisions or the versions
	 * @throws BuildException
	 *             -
	 */
	public void produceList(File pomFile, CompiledArtifact terminalBuildArtifact, List<AnalysisArtifact> solutions, File outputFile)
			throws BuildException {

		Packaging packaging = Packaging.T.create();

		if (terminalBuildArtifact != null) {
			com.braintribe.model.packaging.Artifact terminalArtifact = com.braintribe.model.packaging.Artifact.T.create();
			transferIdentification(terminalArtifact, terminalBuildArtifact);
			packaging.setTerminalArtifact(terminalArtifact);
		}

		packaging.setTimestamp(new Date());

		Comparator<AnalysisArtifact> comparator = Comparator.comparing(AnalysisArtifact::getArtifactId).thenComparing(AnalysisArtifact::getGroupId)
				.thenComparing(AnalysisArtifact::getVersion);

		// copy to keep parameter immutable
		solutions = new ArrayList<>(solutions);
		// sort solutions first..
		Collections.sort(solutions, comparator);
		String root = null;

		StringBuilder hashBuilder = new StringBuilder();
		Set<com.braintribe.model.packaging.Dependency> packagedArtifacts = new TreeSet<com.braintribe.model.packaging.Dependency>(
				new ArtifactComparator());
		for (AnalysisArtifact solution : solutions) {

			com.braintribe.model.packaging.Dependency artifact = com.braintribe.model.packaging.Dependency.T.create();
			transferIdentification(artifact, solution);
			packagedArtifacts.add(artifact);

			if (hashBuilder.length() > 0) {
				hashBuilder.append(",");
			}
			hashBuilder.append(solution.getVersion());
		}
		packaging.setDependencies(new ArrayList<com.braintribe.model.packaging.Dependency>(packagedArtifacts));

		// get revision info
		packaging.setRevision("-1");
		try {
			String md5 = MD5HashGenerator.MD5(hashBuilder.toString(), "UTF-8");
			packaging.setMD5(md5);
		} catch (CryptoServiceException e1) {
			String msg = "cannot generated MD5 hash from solutions";
			throw new BuildException(msg, e1);
		}

		// set version (i.e. release/snapshot name)
		packaging.setVersion(getReleaseVersionString(packaging.getTimestamp()));
		StaxMarshaller marshaller = new StaxMarshaller();

		try (OutputStream out = new FileOutputStream(outputFile)) {
			marshaller.marshall(out, packaging, GmSerializationOptions.deriveDefaults().outputPrettiness(OutputPrettiness.high).build());
		} catch (Exception e) {
			String msg = "cannot write packaging data to [" + outputFile.getAbsolutePath() + "]";
			throw new BuildException(msg, e);
		}

	}

	/**
	 * Creates a version string to be used as a name/identification for the build/release (see
	 * {@link Packaging#getVersion()}. This method by default returns a string consisting of a prefix, a timestamp and the
	 * user name: <code>localsnapshot+[timestamp]_[user]</code>. This clearly marks the build/release as local (snapshot)
	 * build. Such snapshots should not be used in any customer/partner environments! Alternatively the CI may set
	 * environment variable "BTANTTASKS__PACKAGING_VERSION" to force a certain version.
	 */
	private static String getReleaseVersionString(Date timestamp) {
		String version = System.getenv("BTANTTASKS__PACKAGING_VERSION");

		if (version == null) {
			String timestampString = DateTools.encode(timestamp, DateTools.TERSE_DATETIME_FORMAT);
			String user = System.getProperty("user.name");
			version = "localsnapshot+" + timestampString + "_" + user;
		}

		return version;
	}

	/**
	 * transfer artifact data from {@link Artifact} to {@link com.braintribe.model.packaging.Artifact}
	 * 
	 * @param artifact
	 *            - the target {@link com.braintribe.model.packaging.Artifact}
	 * @param identification
	 *            - the source {@link Artifact}
	 */
	private void transferIdentification(com.braintribe.model.packaging.Artifact artifact, CompiledArtifact identification) {
		artifact.setGroupId(identification.getGroupId());
		artifact.setArtifactId(identification.getArtifactId());
		artifact.setVersion(identification.getVersion().asString());
	}

	private void transferIdentification(com.braintribe.model.packaging.Artifact artifact, Artifact identification) {
		artifact.setGroupId(identification.getGroupId());
		artifact.setArtifactId(identification.getArtifactId());
		artifact.setVersion(identification.getVersion());
	}

	private static class ArtifactComparator implements Comparator<com.braintribe.model.packaging.Artifact> {
		@Override
		public int compare(com.braintribe.model.packaging.Artifact o1, com.braintribe.model.packaging.Artifact o2) {
			return o1.getArtifactId().compareTo(o2.getArtifactId());
		}
	}

}
