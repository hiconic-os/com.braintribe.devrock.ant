// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.validation;

import java.io.File;

import org.apache.tools.ant.Task;

import com.braintribe.cfg.Configurable;
import com.braintribe.devrock.model.mc.reason.PomValidationReason;

/**
 * @author peter.gazdik
 */
public abstract class AbstractPomValidatingTask extends Task {

	protected File pomFile;

	@Configurable
	public void setPomFile(File pomFile) {
		this.pomFile = pomFile;
	}

	public abstract PomValidationReason runValidation();

}
