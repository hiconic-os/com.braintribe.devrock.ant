// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;

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
import com.braintribe.utils.FileTools;

/**
 * Assigns a {@link RepositoryView#setDisplayName() displayName} to a {@link RepositoryView}.
 * 
 * @author dirk.scheffler
 */
public class NameReleaseViewTask extends Task {

	private String displayName;
	private File file;
	private File output;

	@Required
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	@Required
	public void setFile(File file) {
		this.file = file;
	}
	
	@Required
	public void setOutput(File output) {
		this.output = output;
	}

	@Override
	public void execute() throws BuildException {
		DrAntTools.runAndPrintStacktraceIfNonBuildException(this::_execute);
	}

	private void _execute() throws BuildException {
		if (file == null)
			throw new BuildException("Argument [file] is mandatory");
		
		if (output == null)
			throw new BuildException("Argumen [output] is mandatory");
		
		var repositoryView = YamlConfigurations.read(RepositoryView.T).from(file).get();
		repositoryView.setDisplayName(displayName);
		
		FileTools.write(output).usingOutputStream( //
			os -> new YamlMarshaller().marshall(os, repositoryView, GmSerializationOptions.defaultOptions.derive() //
				.set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic) //
				.writeEmptyProperties(false) //
				.writeAbsenceInformation(false) //
				.build() //
		));

	}
}
