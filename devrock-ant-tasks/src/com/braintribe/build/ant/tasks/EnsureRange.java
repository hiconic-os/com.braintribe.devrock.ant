// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.utils.lcd.CommonTools.isEmpty;

import java.io.File;
import java.util.function.Predicate;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.types.RangeExpander;
import com.braintribe.build.ant.types.RangeExpander.RangeExpanderResult;
import com.braintribe.build.ant.types.RangeExpander.SolutionPostProcessor;
import com.braintribe.build.ant.utils.DependerResolver;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.common.RegexCheck;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.core.commons.McReasonOutput;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonException;

/**
 * if input property is ".", the tasks is enumerating all artifacts in either the group folder or the codebase root
 * folder, and returns this as the output property
 * 
 * @author pit
 * 
 *         to debug: set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 */
public class EnsureRange extends Task {

	private String input;
	private File rootFile;
	private String outputProperty = "ensuredRange";
	private String ignoreProperty = "ignoreRange";
	private boolean expand = false;

	@Required
	public void setRoot(String root) {
		this.rootFile = new File(root);
	}

	@Required
	public void setInput(String inputProperty) {
		this.input = inputProperty;
	}

	@Configurable
	public void setExpand(boolean expand) {
		this.expand = expand;
	}

	@Configurable
	public void setOutputProperty(String outputProperty) {
		this.outputProperty = outputProperty;
	}

	@Configurable
	public void setIgnoreProperty(String ignoreProperty) {
		this.ignoreProperty = ignoreProperty;
	}

	private static Predicate<String> ALWAYS_TRUE = s -> true;
	private static SolutionPostProcessor IDENTITY = (list, defaultGroupId) -> list;

	@Override
	public void execute() throws BuildException {
		try {
			_execute();
		}
		catch (ReasonException e) {
			Reason reason = e.getReason();
			
			ConsoleOutputs.println(sequence(
					brightRed("Error:\n"),
					new McReasonOutput().output(reason)
			));
			
			throw new BuildException("Error while ensuring range. See error above!");
		}
	}

	private void _execute() {
		boolean brackets = input.startsWith("[") && input.endsWith("]");
		String inputWithoutBrackets = brackets ? input.substring(1, input.length() - 1) : input;
		Predicate<String> artifactMatcher = inputWithoutBrackets.startsWith("!") ? unitTestAcceptingMatcher() : ALWAYS_TRUE;
		SolutionPostProcessor solutionPostProcessor = IDENTITY;

		if (requiresDependersOnly()) {
			brackets = true;
			inputWithoutBrackets = input.substring(2);
			solutionPostProcessor = new DependerResolver(inputWithoutBrackets);

		} else if (input.contains("*")) {
			if (input.contains("\\"))
				throw new IllegalArgumentException("range contained illegal characters: '" + input + "'.");

			String finalRegex = "\\Q" + inputWithoutBrackets.replaceAll("\\*", "\\\\E.*\\\\Q") + "\\E";
			artifactMatcher = regexChecker(finalRegex, null);

		} else if (!needsRangeExpander(inputWithoutBrackets)) {
			setOutput(input);
			setEmptyIgnore();
			return;
		}

		RangeExpanderResult result = new RangeExpander().determineRange(rootFile, expand, brackets, artifactMatcher, solutionPostProcessor);

		if (!isEmpty(result.range) && !result.range.equals("[]"))
			setOutput(result.range);

		if (!isEmpty(result.dontbuilds))
			setIgnore(result);
		else {
			setEmptyIgnore();
		}
	}

	private boolean requiresDependersOnly() {
		return input.startsWith("]]") || input.startsWith("!]");
	}

	private RegexCheck unitTestAcceptingMatcher() {
		return regexChecker(".*-test|.*Test", ".*-integration-test|.*IntegrationTest|.*-end2end-test");
	}

	private RegexCheck regexChecker(String include, String exclude) {
		RegexCheck result = new RegexCheck();
		result.setIncludeRegex(include);
		result.setExcludeRegex(exclude);
		return result;
	}

	private boolean needsRangeExpander(String rangeExpression) {
		return ".".equals(rangeExpression) || "!".equals(rangeExpression);
	}

	private void setOutput(String range) {
		getProject().setProperty(outputProperty, range);
	}

	private void setIgnore(RangeExpanderResult result) {
		getProject().setProperty(ignoreProperty, result.dontbuilds);
	}
	
	private void setEmptyIgnore() {
		getProject().setProperty(ignoreProperty, "");	
	}

}
