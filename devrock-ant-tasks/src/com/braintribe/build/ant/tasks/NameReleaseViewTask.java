// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.utils.DrAntTools;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.devrock.model.repositoryview.RepositoryView;
import com.braintribe.gm.config.yaml.YamlConfigurations;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.utils.FileTools;

/**
 * Assigns a {@link RepositoryView#setDisplayName() displayName} to a {@link RepositoryView}.
 * 
 * @author dirk.scheffler
 */
public class NameReleaseViewTask extends Task {

	private File file;
	private File output;
	private File releaseNotesOutput;
	private File releaseNotesFile;
	private Pom pom;

	@Required
	public void addPom( Pom pom) {
		this.pom = pom;
	}

	@Required
	public void setFile(File file) {
		this.file = file;
	}
	
	@Required
	public void setOutput(File output) {
		this.output = output;
	}
	
	@Required
	public void setReleaseNotesFile(File releaseNotesFile) {
		this.releaseNotesFile = releaseNotesFile;
	}
	
	@Required
	public void setReleaseNotesOutput(File releaseNotesOutput) {
		this.releaseNotesOutput = releaseNotesOutput;
	}

	@Override
	public void execute() throws BuildException {
		DrAntTools.runAndPrintStacktraceIfNonBuildException(this::_execute);
	}

	private void _execute() throws BuildException {
		if (pom == null)
			throw new BuildException("Argument [pom] is mandatory");
		
		if (file == null)
			throw new BuildException("Argument [file] is mandatory");
		
		if (output == null)
			throw new BuildException("Argumen [output] is mandatory");
		
		if (releaseNotesFile == null)
			throw new BuildException("Argument [releaseNotesFile] is mandatory");
		
		if (releaseNotesOutput == null)
			throw new BuildException("Argumen [releaseNotesOutput] is mandatory");
		
		var repositoryView = YamlConfigurations.read(RepositoryView.T).from(file).get();
		
		CompiledArtifactIdentification artifact = CompiledArtifactIdentification.from(pom.getArtifact());
		
		var displayName = artifact.asString();
		
		repositoryView.setDisplayName(displayName);
		
		FileTools.write(output).usingOutputStream( //
			os -> new YamlMarshaller().marshall(os, repositoryView, GmSerializationOptions.defaultOptions.derive() //
				.set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic) //
				.writeEmptyProperties(false) //
				.writeAbsenceInformation(false) //
				.build() //
		));
		
		var releaseNotes = FileTools.read(releaseNotesFile).withCharset(StandardCharsets.UTF_8).asString();
		
		if (releaseNotes.isEmpty())
			releaseNotes = "# Release ${groupId}:${artifactId}#${version}";

		Map<String, String> variables = new HashMap<>(pom.getArtifact().getProperties());
		variables.put(VersionedArtifactIdentification.groupId, artifact.getGroupId());
		variables.put(VersionedArtifactIdentification.artifactId, artifact.getArtifactId());
		variables.put(VersionedArtifactIdentification.version, artifact.getVersion().asString());
		releaseNotes = ReasonedTemplating.merge(releaseNotes, variables).get();

		FileTools.write(releaseNotesOutput).withCharset(StandardCharsets.UTF_8).string(releaseNotes);
	}
}
