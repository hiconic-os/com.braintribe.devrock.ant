package com.braintribe.build.ant.tasks.validation;

import java.io.File;
import java.util.List;

import org.apache.tools.ant.BuildException;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.cfg.Configurable;
import com.braintribe.devrock.model.mc.reason.PomValidationReason;
import com.braintribe.devrock.model.mc.reason.UnresolvedDependency;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependency;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;

/**
 * simple task that validates if all references to parents, imports and dependencies can be resolved and are present at the time of running the task
 * 
 * @author pit
 *
 */
public class PomContentValidatingTask extends AbstractPomValidatingTask {

	private PomValidationReason pvr;

	private String exposureProperty = "pomContentValidationResult";

	@Configurable
	public void setExposureProperty(String exposureProperty) {
		this.exposureProperty = exposureProperty;
	}

	@Override
	public void execute() throws BuildException {
		PomValidationReason reason = runValidation();
		if (reason != null) {
			getProject().setProperty(exposureProperty, reason.stringify());
			throw new BuildException("file [" + pomFile.getAbsolutePath() + "] is not valid : " + reason.stringify());
		}
	}

	@Override
	public PomValidationReason runValidation() {
		if (pomFile == null)
			throw new BuildException("No pom file passed");

		if (!pomFile.exists())
			throw new BuildException("Passed pom file [" + pomFile.getAbsolutePath() + "] doesn't exist");

		McBridge mcBridge = Bridges.getInstance(getProject());
		// read pom
		CompiledArtifact ca = mcBridge.readArtifact(pomFile);

		// parent reference
		CompiledDependencyIdentification parentCdi = ca.getParent();
		if (parentCdi != null) {
			validateDependency(mcBridge, parentCdi, "parent reference");
		}
		List<CompiledDependencyIdentification> imports = ca.getImports();
		if (imports != null && imports.size() > 0) {
			for (CompiledDependencyIdentification importCdi : imports) {
				validateDependency(mcBridge, importCdi, "import reference");
			}
		}

		List<CompiledDependency> dependencies = ca.getDependencies();
		if (dependencies != null && dependencies.size() > 0) {
			for (CompiledDependencyIdentification cdi : dependencies) {
				validateDependency(mcBridge, cdi, "dependency");
			}
		}

		return pvr;

	}

	private void validateDependency(McBridge mcBridge, CompiledDependencyIdentification cdi, String tag) {
		Maybe<CompiledArtifactIdentification> parentCaiMaybe = mcBridge.resolveDependencyAsMaybe(cdi);
		if (!parentCaiMaybe.isSatisfied()) {
			UnresolvedDependency r = Reasons.build(UnresolvedDependency.T) //
					.text(tag + ": no matching artifact identification for [" + cdi.asString() + "]") //
					.toReason();
			acquirePomValidationReason(pomFile).getReasons().add(r);

		} else {
			CompiledArtifactIdentification parentCai = parentCaiMaybe.get();
			Maybe<CompiledArtifact> caMaybe = mcBridge.resolveArtifactAsMaybe(parentCai);
			if (!caMaybe.isSatisfied()) {
				UnresolvedDependency r = Reasons.build(UnresolvedDependency.T).text(tag + ": unresolved artifact for [" + parentCai.asString() + "]")
						.toReason();
				acquirePomValidationReason(pomFile).getReasons().add(r);
			}
		}
	}

	private PomValidationReason acquirePomValidationReason(File pomFile) {
		if (pvr != null)
			return pvr;
		pvr = Reasons.build(PomValidationReason.T).text("invalid pom [" + pomFile.getAbsolutePath() + "]").toReason();
		return pvr;
	}

}
