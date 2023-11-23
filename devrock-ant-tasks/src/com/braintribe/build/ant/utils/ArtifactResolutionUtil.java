package com.braintribe.build.ant.utils;

import static com.braintribe.console.ConsoleOutputs.brightBlack;
import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.green;
import static com.braintribe.console.ConsoleOutputs.text;
import static com.braintribe.console.ConsoleOutputs.yellow;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;
import com.braintribe.console.output.ConsoleOutput;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.analysis.AnalysisTerminal;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledDependency;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.pr.AbsenceInformation;
import com.braintribe.model.generic.pr.criteria.matching.Matcher;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.StandardCloningContext;
import com.braintribe.model.generic.reflection.TraversingContext;
import com.braintribe.utils.lcd.StringTools;

public class ArtifactResolutionUtil {
	
	public static AnalysisArtifactResolution trimToFailures(AnalysisArtifactResolution resolution) {
		return resolution.clone(new FailureTrimmingContext(resolution));
	}
	
	private static class FailureTrimmingContext extends StandardCloningContext implements Matcher {
		private Set<ArtifactIdentification> failureContext = new HashSet<>();
		private AnalysisArtifactResolution resolution;
		
		public FailureTrimmingContext(AnalysisArtifactResolution resolution) {
			this.resolution = resolution;
			
			
			setMatcher(this);
			for (AnalysisArtifact artifact: resolution.getIncompleteArtifacts()) {
				collectFailureContext(artifact);
			}
			collectFailureContext(resolution.getUnresolvedDependencies());
		}
		
		private void collectFailureContext(AnalysisDependency dependency) {
			if (!failureContext.add(dependency))
				return;
			
			AnalysisArtifact dependerArtifact = dependency.getDepender();
			collectFailureContext(dependerArtifact);
		}
		
		private void collectFailureContext(Collection<AnalysisDependency> dependencies) {
			for (AnalysisDependency dependency: dependencies) {
				collectFailureContext(dependency);
			}
		}
		
		private void collectFailureContext(AnalysisArtifact artifact) {
			// TODO: revise this ... if a terminal itself has an issue in the CPR (here, it's used to get a jar) 
			// it is not properly collected here, because the terminal has depender (a dependency) but this 
			// dependency has no depender (artifact) hence this leads to a NPE. 			
			if (artifact == null) {
				return;
			}
			if (!failureContext.add(artifact))
				return;

			collectFailureContext(artifact.getDependers());
		}

		@Override
		public boolean canTransferPropertyValue(EntityType<? extends GenericEntity> entityType, Property property,
				GenericEntity instanceToBeCloned, GenericEntity clonedInstance,
				AbsenceInformation sourceAbsenceInformation) {
			
			return !property.getName().equals("origin");
		}
		
		@Override
		public boolean matches(TraversingContext traversingContext) {
			Object value = traversingContext.getObjectStack().peek();
			
			if (value instanceof ArtifactIdentification) {
				return !failureContext.contains((ArtifactIdentification)value);
			}
			
			return false;
		}
	}
	
	
	private static final String entryEdge = "\u2500\u2500\u2500";
	private static final String edge = "\u251C\u2500\u2500";
	private static final String lastEdge = "\u2514\u2500\u2500";
	private static final String connector = "\u2502  ";
	private static final String empty = "   ";
	private static final String check = "\u221a";
	private static final String mark = "\u2666";
	

	
	public static void printDependencyTree(AnalysisArtifactResolution resolution) {
		
		StringBuilder padding = new StringBuilder();
		ConfigurableConsoleOutputContainer outputs = ConsoleOutputs.configurableSequence();
		Set<AnalysisArtifact> visited = new HashSet<>();

		for (AnalysisTerminal terminal: resolution.getTerminals()) {
			outputs.append(brightBlack(entryEdge));
			if (terminal instanceof AnalysisDependency) {
				AnalysisDependency terminalDependency = (AnalysisDependency)terminal;
				printDependencyTree(terminalDependency, terminalDependency.getSolution(), outputs, padding, false, visited);
			}
			else {
				AnalysisArtifact terminalArtifact = (AnalysisArtifact)terminal;
				printDependencyTree(null, terminalArtifact, outputs, padding, false, visited);
			}
		}
		
		ConsoleOutputs.println(outputs);
	}
	
	private static void printDependencyTree(AnalysisDependency dependency, AnalysisArtifact artifact, ConfigurableConsoleOutputContainer outputs, StringBuilder padding, boolean hasSuccessors, Set<AnalysisArtifact> visited) {
		if (artifact == null) {
			ConfigurableConsoleOutputContainer outputDependency = outputDependency(dependency);
			outputDependency.append(ConsoleOutputs.cyan(" -> "));
			outputDependency.append(ConsoleOutputs.brightRed("unresolved"));
			outputs.append(outputDependency);
			outputs.append("\n");
			return;
		}
		
		boolean alreadyTraversed = !visited.add(artifact);
		
		outputs.append(artifactNameColoredByType(dependency, artifact, alreadyTraversed)).append("\n");
		
		if (alreadyTraversed)
			return;

		List<AnalysisDependency> dependencies = artifact.getDependencies();
		
		int dependencyCount = dependencies.size();
		
		int i = 0;
		for (AnalysisDependency artifactDependency: dependencies) {
			boolean hasChildSuccessors = i++ < (dependencyCount - 1);
			if (hasSuccessors) {
				// has successive siblings
				padding.append(connector);
			}
			else {
				// last dependency
				padding.append(empty);
			}
			
			outputs.append(brightBlack(padding.toString())).append(brightBlack(hasChildSuccessors? edge: lastEdge));
			printDependencyTree(artifactDependency, artifactDependency.getSolution(), outputs, padding, hasChildSuccessors, visited);
			
			padding.setLength(padding.length() - 3);
		}
	}
	
	private static ConsoleOutput artifactNameColoredByType(AnalysisDependency dependency, AnalysisArtifact artifact, boolean alreadyTraversed) {

		ConfigurableConsoleOutputContainer solutionOutput = ConsoleOutputs.configurableSequence();
		
		if (dependency != null) {
			solutionOutput.append(outputDependency(dependency));
			solutionOutput.append(ConsoleOutputs.cyan(" -> "));
			solutionOutput.append(outputArtifact(dependency, artifact));
		}
		else {
			solutionOutput.append(outputArtifact(artifact));
		}

		if (alreadyTraversed) {
			solutionOutput.append(ConsoleOutputs.yellow(" already traversed"));
		}

		ConfigurableConsoleOutputContainer detailInfo = ConsoleOutputs.configurableSequence();

		solutionOutput.append(brightBlack(detailInfo));
		

		//detailInfo.append(" -> ").append(ConsoleOutputs.cyan(artifact.getNature().entityType().getShortName())).append(brightBlack(" -> "));

		return solutionOutput;
	}
	
	public static ConfigurableConsoleOutputContainer outputTerminal(CompiledTerminal terminal) {
		if (terminal instanceof CompiledDependencyIdentification) {
			return outputDependency((CompiledDependencyIdentification) terminal);
		}
		else {
			return outputArtifact((CompiledArtifact) terminal);
		}
	}
	
	public static ConfigurableConsoleOutputContainer outputTerminal(AnalysisTerminal terminal) {
		if (terminal instanceof AnalysisDependency) {
			return outputDependency((AnalysisDependency) terminal);
		}
		else {
			return outputArtifact((AnalysisArtifact) terminal);
		}
	}
	
	public static ConfigurableConsoleOutputContainer outputDependency(AnalysisDependency dependency) {
		String groupId = dependency.getGroupId();
		String artifactId = dependency.getArtifactId();
		String version = dependency.getVersion();
		String type = PartIdentification.asString(dependency);
		String scope = dependency.getScope();
		
		return outputArtifact(groupId, artifactId, version, scope, type, dependency.hasFailed());
	}
	
	public static ConfigurableConsoleOutputContainer outputArtifact(AnalysisArtifact artifact) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();
		String packaging = artifact.getPackaging();
		
		return outputArtifact(groupId, artifactId, version, null, packaging, artifact.hasFailed());
	}
	
	public static ConfigurableConsoleOutputContainer outputArtifact(Artifact artifact) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();
		String packaging = artifact.getPackaging();
		
		return outputArtifact(groupId, artifactId, version, null, packaging, false);
	}
	
	public static ConfigurableConsoleOutputContainer outputDependency(CompiledDependencyIdentification dependency) {
		String groupId = dependency.getGroupId();
		String artifactId = dependency.getArtifactId();
		String version = dependency.getVersion().asString();
		String type = null;
		String scope = null;
		if (dependency instanceof CompiledDependency) {
			CompiledDependency compiledDependency = (CompiledDependency)dependency;
			scope = compiledDependency.getScope();
			type = PartIdentification.asString(compiledDependency);
		}
		
		return outputArtifact(groupId, artifactId, version, scope, type, false);
	}
	
	public static ConfigurableConsoleOutputContainer outputArtifact(CompiledArtifact artifact) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion().asString();
		String packaging = artifact.getPackaging();
		
		return outputArtifact(groupId, artifactId, version, null, packaging, false);
	}
	
	public static ConfigurableConsoleOutputContainer outputArtifact(AnalysisDependency dependency, AnalysisArtifact artifact) {
		String groupId = artifact.getGroupId();
		String artifactId = artifact.getArtifactId();
		String version = artifact.getVersion();
		String packaging = artifact.getPackaging();
		
		if (groupId.equals(dependency.getGroupId())) {
			groupId = null;
		}
		
		if (!artifact.hasFailed() && artifactId.equals(dependency.getArtifactId())) {
			artifactId = null;
		}
		
		return outputArtifact(groupId, artifactId, version, null, packaging, artifact.hasFailed());
	}
	
	/** {@code groupId}, {@code artifactId} and {@code version} are all optional. */
	public static ConfigurableConsoleOutputContainer outputArtifact(String groupId, String artifactId, String version, String scope, String type, boolean error) {
		ConfigurableConsoleOutputContainer configurableSequence = ConsoleOutputs.configurableSequence();
		if (error)
			configurableSequence.append(brightRed("! "));
			
		if (groupId != null)
			configurableSequence.append(brightBlack(groupId + ":"));

		if (artifactId != null)
			configurableSequence.append(text(artifactId));

		if (!StringTools.isEmpty(version))
			configurableSequence //
					.append(brightBlack("#")) //
					.append(isSnapshot(version) ? yellow(version) : green(version));
		
		if (scope != null)
			configurableSequence.append(brightBlack(" " + scope));
		
		if (type != null) 
			configurableSequence.append(brightBlack(" " + type));

		return configurableSequence;
	}

	private static boolean isSnapshot(String version) {
		return version.endsWith("-pc") || version.endsWith("-SNAPSHOT");
	}
	
}
