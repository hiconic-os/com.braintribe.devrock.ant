// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;
import com.braintribe.build.ant.tasks.extractor.fileset.ResourceCollectionTargetProcessor;
import com.braintribe.build.ant.tasks.malaclypse.ArtifactExclusionList;
import com.braintribe.build.ant.tasks.malaclypse.ArtifactListProducer;
import com.braintribe.build.ant.tasks.malaclypse.LegacySolutionListTransposer;
import com.braintribe.build.ant.types.FileSetTarget;
import com.braintribe.build.ant.types.ResolutionFileType;
import com.braintribe.build.ant.utils.ArtifactResolutionUtil;
import com.braintribe.build.ant.utils.ParallelBuildTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.OutputPrettiness;
import com.braintribe.codec.marshaller.api.TypeExplicitness;
import com.braintribe.codec.marshaller.api.TypeExplicitnessOption;
import com.braintribe.codec.marshaller.stax.StaxMarshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.common.lcd.Pair;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;
import com.braintribe.model.artifact.PartTuple;
import com.braintribe.model.artifact.PartType;
import com.braintribe.model.artifact.Solution;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.analysis.AnalysisTerminal;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.consumable.LinkedArtifact;
import com.braintribe.model.artifact.consumable.LinkedArtifactResolution;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.pr.criteria.TraversingCriterion;
import com.braintribe.model.generic.pr.criteria.matching.StandardMatcher;
import com.braintribe.model.generic.processing.pr.fluent.TC;
import com.braintribe.model.generic.reflection.BaseType;
import com.braintribe.model.generic.reflection.ConfigurableCloningContext;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.generic.reflection.StrategyOnCriterionMatch;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;

/**
 * 
 * Malaclypse's dependency walk in an ant task..  
 *
 * required parameters: 	
 *	
 *  artifact : the condensed name of the terminal artifact - pom file is deduced (must reside in the repository !!)  
 *  
 * OR
 * 
 *	pomFile : the pom file that denotes the terminal artifact
 *		like "d:/works/svn/development/artifacts/com/braintribe/model/ProcessModel/1.0/pom.xml"
 * OR
 * 	
 *  child list of dependencies (requires installed artifacts) 
 *  like 
 *  <bt:dependencies>
 *  	<dependency groupId="" artifactId="" version=""/>
 *  	[<dependency>..</dependency>]
 *  </bt:dependencies>
 *
 * OR
 * 	
 *   dependencies : comma separated list of dependencies (requires installed artifacts) 
 *   	like: com.braintribe:Example#1.2.3;com.braintribe:OtherExample#4.5.6
 *
 * optional parameters: 
 * 
 * type: a comma-delimited list of part tuples or part types 
 * 	part tuple = [<classifier>]:[<extension>], a representation of the {@link PartTuple}
 * 		like "sources:jar",":pom"
 * 
 *  part type = [<part type>] as string values of the enum {@link PartType}
 * 	 	like "jar", "sources", "javadoc" 
 * 
 *  exclusionDependency : the artifact reference of which the .exclusion file is to be read, 
 * 		like "com.braintribe.build.plattform:CSP#4.0.^" 
 * 
 *  packagingFile : the name of the file where the packaging entity is going to be encoded by the GenericModelEntityDomCodec,
 *  	like "packaging.xml"
 *  
 *  pathId : the id of the path variable
 *  	like "classpath"
 *  
 *	filesetId : the id of the file set with the *.jar 
 *		like "classpathFileSet" 
 *
 *  sourcesFilesetId : the id of the file set with the *-sources.jar
 *  	like "sourceFileSet"  	 
 *  
 *	solutionListFile : an name of the file where the list of solutions is to be encoded by the GenericModelEntityDomCodec 
 *		like "solutions.xml"
 *	
 *	solutionListStringProperty : the name of the property where the list of solutions is to be stored.
 *		like "solutions"
 *
 * 	localRepositoryProperty : the name of the property where the path of the local repository is to be stored.
 *		like "localRepository"
 *
 *  useScope : the name of the magic scope that should be used
 *  	default is "runtime"
 *  	like : "compile" | "runtime" 
 *  		compile : compile & provided
 *  		runtime : compile & runtime
 *  	
 *
 *  other parameters that have a default value:
 *
 *  addSelf : will add the terminal artifact to the solution list (but only POM and JAR) : default is false
 *  
 *  other features:
 *  
 *  DenotationType can be overloaded either by specifying a json string or xml structure. Default is launch, include optionals  
 * 
 *  to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 *	com.braintribe.csp.server:Exclusions#4.0.^
 * 
 * @author pit
 *
 */
public class DependenciesTask extends Task implements HasLoggingConfiguration  {

	private String filesetId;
	private String sourcesFilesetId;
	private String javadocFilesetId;
	private String pathId;
	
	private File pomFile;
	private File packagingFile;
	private File solutionListFile;
	private String solutionListStringProperty;
	private String resolutionFile;
	private ResolutionFileType resolutionFileType = ResolutionFileType.ARTIFACTS;
	
	private String localRepositoryProperty;
	
	private String type;
	private String tagRule;
	
	private final ArtifactListProducer packagingListProducer = new ArtifactListProducer();
	
	private String exclusionDependency;
	
	
	private String artifact;
	
	
	private final List<com.braintribe.build.ant.types.Dependency> injectedDependencies = new ArrayList<com.braintribe.build.ant.types.Dependency>();
	private Pom pom;
	private final ResourceCollectionTargetProcessor targetProcessor = new ResourceCollectionTargetProcessor();
	
	private String useScope = "runtime";
	private String typeFilter = "jar";
	private String resolutionId;
	
	@Configurable
	public void setResolutionId(String resolutionId) {
		this.resolutionId = resolutionId;
	}
	@Configurable
	public void setType(String type) {
		this.type = type;
	}
	
	@Configurable @Required
	public void setFilesetId(String filesetId) {
		this.filesetId = filesetId;
	}
	@Configurable
	public void setSourcesFilesetId(String sourcesFilesetId) {
		this.sourcesFilesetId = sourcesFilesetId;
	}
	@Configurable
	public void setJavadocFilesetId(String javadocFilesetId) {
		this.javadocFilesetId = javadocFilesetId;
	}
	@Configurable
	public void setPathId(String pathId) {
		this.pathId = pathId;
	}		
	@Configurable
	public void setPomFile(File pomFile) {
		this.pomFile = pomFile;
	}	
	@Configurable
	public void setPackagingFile(File packagingFile) {
		this.packagingFile = packagingFile;
	}	
	@Configurable
	public void setSolutionListFile(File solutionListFile) {
		this.solutionListFile = solutionListFile;
	}
	@Configurable
	public void setSolutionListStringProperty(String solutionListStringProperty) {
		this.solutionListStringProperty = solutionListStringProperty;
	}
	
	@Configurable
	public void setResolutionFile(String resolutionFile) {
		this.resolutionFile = resolutionFile;
	}
	
	@Configurable
	public void setResolutionFileType(String resolutionFileType) {
		this.resolutionFileType = ResolutionFileType.valueOf(resolutionFileType);
	}
	
	@Configurable
	public void setLocalRepositoryProperty(String localRepositoryProperty) {
		this.localRepositoryProperty = localRepositoryProperty;
	}
	@Configurable
	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	/** @deprecated This never had any effect... */
	@Deprecated
	@Configurable
	public void setAddSelf(@SuppressWarnings("unused") boolean addSelf) {
		/* This never did anything...*/
	}
	@Configurable
	public void setExclusionDependency(String exclusionDependency) {
		this.exclusionDependency = exclusionDependency;
	}
	
	@Configurable
	public void setUseScope(String scope) {
		useScope = scope;
	}
	
	@Configurable
	public void setTagRule( String rule) {
		this.tagRule = rule;
	}
	
	@Configurable
	public void setTypeFilter( String filter) {
		this.typeFilter = filter;
	}
	
	public void addConfiguredDependency(com.braintribe.build.ant.types.Dependency dependency) {
		injectedDependencies.add( dependency);
	}	
	public void addDependency( com.braintribe.build.ant.types.Dependency dependency) {
		injectedDependencies.add( dependency);
	}
	
	public void setDependencies(String dependencies) {
		for (String dependencyString : StringTools.splitSemicolonSeparatedString(dependencies, false)) {
			com.braintribe.build.ant.types.Dependency dependency = new com.braintribe.build.ant.types.Dependency();
			dependency.setCondensedArtifactName(dependencyString);
			injectedDependencies.add( dependency);
		}
	}
	
	public void addPom( Pom pom) {
		this.pom = pom;
	}
	
	public void addFilesetTarget( FileSetTarget target) {
		targetProcessor.addTarget( target);
	}
	
	@Override
	public void execute() throws BuildException {
		ParallelBuildTools.runGloballySynchronizedRepoRelatedTask(this::_execute);
	}
	
	private void _execute() throws BuildException {
		initalizeLogging();
		new ColorSupport(getProject()).installConsole();
		
		McBridge mcBridge = Bridges.getInstance(getProject());
				
		final List<CompiledTerminal> terminals = new ArrayList<>();
		final CompiledArtifact terminalArtifact; 
		
		if (this.artifact != null) {
			terminalArtifact = mcBridge.resolveArtifact(CompiledArtifactIdentification.parse(this.artifact));
			terminals.add(terminalArtifact);

		} else   {
			// walk mode with pom file passed. 
			if (pomFile == null && pom != null) {
				// ant doesn't call execute on the inner task, so we might need to do it ourselfs
				pom.execute();
				pomFile = pom.getFile();
			}

			if (pomFile == null) {
				terminalArtifact = null;

			} else {
				if (!pomFile.exists())
					throw new BuildException("File [" + pomFile.getAbsolutePath() + "] doesn't exist!");
				
				terminalArtifact = mcBridge.readArtifact(pomFile);
				terminals.add(terminalArtifact);
			}
		}

		if (!injectedDependencies.isEmpty())
			injectedDependencies.stream() //
					.map(com.braintribe.build.ant.types.Dependency::asCompiledDependency) //
					.forEach(terminals::add);

		else if (terminals.isEmpty())
			throw new BuildException("Cannot resolve dependencies, no artifact, pom or explicit dependencies were set.");

		ConfigurableConsoleOutputContainer intro = ConsoleOutputs.configurableSequence();
		
		intro.append("Resolving: ");
		
		boolean first = true;
		for (CompiledTerminal terminal: terminals) {
			if (first)
				first = false;
			else
				intro.append(", ");
			intro.append(ArtifactResolutionUtil.outputTerminal(terminal));
		}
		ConsoleOutputs.println(intro);

		
		File localRepository = mcBridge.getLocalRepository();
		
		if (localRepositoryProperty != null) {
			getProject().setProperty(localRepositoryProperty, localRepository.getAbsolutePath());
		}
		
		Project project = getProject();
			
		//
		// setup the file set target processor with all three file set ids and the type property
		//
	
		targetProcessor.setupWithStandardFilesetIds( filesetId, sourcesFilesetId, javadocFilesetId, type, pathId);
						
		// exclusion list 
		Set<ArtifactIdentification> exclusions = null;
		if (exclusionDependency != null) {		
			
			PartIdentification exclusionsPart = PartIdentification.create("exclusions");
			
			com.braintribe.model.artifact.consumable.Artifact exclusionArtifact = mcBridge.resolveArtifact(CompiledDependencyIdentification.parse(exclusionDependency), exclusionsPart);
			
			com.braintribe.model.artifact.consumable.Part part = exclusionArtifact.getParts().get(exclusionsPart.asString());
			
			if (part == null)
				throw new IllegalStateException("Missing exclusions part in artifact: " + exclusionArtifact.asString());
			
			
			try (InputStream in = part.getResource().openStream()) {
				exclusions = new ArtifactExclusionList( IOTools.slurp(in, "UTF-8")).getExclusions();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		List<PartIdentification> relevantPartTuples = targetProcessor.getRelevantPartTuples();

		// TODO: analyze tag rule and typeFilter
		AnalysisArtifactResolution resolution = mcBridge.resolveClasspath(terminals, useScope, tagRule, typeFilter, relevantPartTuples, exclusions);

		
		// 
		// filter solutions for the target processors 
		//  
		for (AnalysisArtifact solution : resolution.getSolutions()) {
			
			for (Part part: solution.getParts().values()) {
				targetProcessor.matchPart(part);
			}
							
		}
			
		
		// export file sets 
		if 	(project != null) {
			targetProcessor.produceFileSets(project);
		}
			
		// TODO: continue here with porting and think of tagrule | typefilter
		
		// export packaging file 
		if (packagingFile != null) {				
			packagingListProducer.produceList( pomFile, terminalArtifact, resolution.getSolutions(), packagingFile);
		}
			
		// temp create a solution file for debug
		if (solutionListFile == null && project != null) {
				try {
					File tfile  = File.createTempFile("bogus.", "");
					File tempDir = tfile.getParentFile();
					tfile.deleteOnExit();																	
				String dir = project.getBaseDir().getPath().replace("\\", ".").replace("/", ".").replace(":", ".");			
				solutionListFile = new File( tempDir, dir + ".xml");
				} catch (IOException e) {
					System.err.println("cannot determine temp directory");
					solutionListFile = null;											
				}						
		}
		
		//
		// export solution list
		//
		
		if (solutionListStringProperty != null) {
			String solutionListString = resolution.getSolutions().stream().map(AnalysisArtifact::asString).sorted().collect(Collectors.joining(","));
			project.setProperty(solutionListStringProperty, solutionListString);
		}
		
		if (solutionListFile != null) {
			LegacySolutionListTransposer transposer = new LegacySolutionListTransposer();
			List<Solution> solutions = transposer.transpose(resolution.getSolutions());
			
			if (solutionListFile != null) {
				solutionListFile.getParentFile().mkdirs();
				StaxMarshaller marshaller = new StaxMarshaller();
				try (OutputStream out = new FileOutputStream( solutionListFile)) {
					marshaller.marshall(out, solutions, GmSerializationOptions.deriveDefaults().outputPrettiness(OutputPrettiness.high).build());						
				} catch (IOException e) {
					String msg ="cannot write solution list to file name [" + solutionListFile.getAbsolutePath() +"]";
					throw new UncheckedIOException( msg, e);
				} 				
			}
		}
		
		if (resolutionFile != null) {
			Pair<Object, GenericModelType> projection = getProjection(resolution);
			YamlMarshaller yamlMarshaller = new YamlMarshaller();
			
			Object assembly = projection.first();
			GenericModelType rootType = projection.second();
			
			try (OutputStream out = new FileOutputStream(resolutionFile)) {
				yamlMarshaller.marshall(out, assembly, GmSerializationOptions.defaultOptions.derive().set(TypeExplicitnessOption.class, TypeExplicitness.polymorphic).inferredRootType(rootType).build());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
		
		
		if (resolutionId != null) {
			getProject().addReference(resolutionId, resolution);
		}
		else {
			getProject().addReference("LAST-DEPENDENCIES-RESOLUTION", resolution);
		}
			
		super.execute();
	}
	
	private Pair<Object, GenericModelType> getProjection(AnalysisArtifactResolution resolution) {
		switch (resolutionFileType) {
		case ANALYSIS_ARTIFACTS:
			return Pair.of(resolution.getSolutions(), GMF.getTypeReflection().getListType(AnalysisArtifact.T));
		case ANALYSIS_ARTIFACT_RESOLUTION:
			return Pair.of(resolution, AnalysisArtifact.T);
		case ANALYSIS_ARTIFACT_RESOLUTION_WITHOUT_ORIGINS:
			return Pair.of(trimOrigins(resolution), AnalysisArtifact.T);
		case ANALYSIS_ARTIFACTS_WITHOUT_ORIGINS:
			return Pair.of(trimOrigins(resolution.getSolutions()), GMF.getTypeReflection().getListType(AnalysisArtifact.T));
		case ARTIFACTS:
			return Pair.of(asArtifacts(resolution), GMF.getTypeReflection().getListType(Artifact.T));
		case ARTIFACT_RESOLUTION:
			return Pair.of(asArtifactResolution(resolution), ArtifactResolution.T);
		case LINKED_ARTIFACTS:
			return Pair.of(asLinkedArtifactResolution(resolution).getSolutions(), GMF.getTypeReflection().getListType(LinkedArtifact.T));
		case LINKED_ARTIFACT_RESOLUTION:
			return Pair.of(asLinkedArtifactResolution(resolution), LinkedArtifactResolution.T);
		default:
			throw new UnsupportedOperationException("Unsupported ResolutionFileType: " + resolutionFileType);
		}
	}

	private <T> T trimOrigins(T assembly) {
		TraversingCriterion tc = TC.create().property("origin").done();
		ConfigurableCloningContext cloningContext = ConfigurableCloningContext.build().withMatcher( StandardMatcher.create(tc)).done();
		return (T)BaseType.INSTANCE.clone(cloningContext, assembly, StrategyOnCriterionMatch.skip);
	}
	
	private LinkedArtifactResolution asLinkedArtifactResolution(AnalysisArtifactResolution resolution) {
		LinkedArtifactResolution artifactResolution = LinkedArtifactResolution.T.create();
		
		Map<AnalysisArtifact, LinkedArtifact> map = new HashMap<>();
		
		List<LinkedArtifact> solutions = artifactResolution.getSolutions();
		
		List<LinkedArtifact> terminalArtifacts = artifactResolution.getTerminals(); 
		
		for (AnalysisTerminal terminal: resolution.getTerminals()) {
			final AnalysisArtifact terminalArtifact;
			if (terminal instanceof AnalysisArtifact) {
				terminalArtifact = (AnalysisArtifact) terminal;
			}
			else {
				terminalArtifact = ((AnalysisDependency)terminal).getSolution();
			}
			
			terminalArtifacts.add(transpose(terminalArtifact, map));
		}
		
		for (AnalysisArtifact solution: resolution.getSolutions()) {
			solutions.add(transpose(solution, map));
		}
		
		return artifactResolution;
	}
	
	private LinkedArtifact transpose(AnalysisArtifact artifact, Map<AnalysisArtifact, LinkedArtifact> map) {
		LinkedArtifact linkedArtifact = map.get(artifact);
		
		if (linkedArtifact == null) {
			linkedArtifact = LinkedArtifact.from(artifact);
			
			for (AnalysisDependency dependency: artifact.getDependencies()) {
				AnalysisArtifact solution = dependency.getSolution();
				
				if (solution != null)
					linkedArtifact.getDependencies().add(transpose(solution, map));
			}
			
			for (AnalysisDependency depender: artifact.getDependers()) {
				AnalysisArtifact solution = depender.getDepender();
				
				if (solution != null)
					linkedArtifact.getDependers().add(transpose(solution, map));
			}
			
			
			map.put(artifact, linkedArtifact);
		}
		
		return linkedArtifact;
	}
	
	private ArtifactResolution asArtifactResolution(AnalysisArtifactResolution resolution) {
		ArtifactResolution artifactResolution = ArtifactResolution.T.create();
		
		Map<AnalysisArtifact, Artifact> map = new HashMap<>();
		
		List<Artifact> solutions = artifactResolution.getSolutions();
		
		List<Artifact> terminalArtifacts = artifactResolution.getTerminals(); 
				
		for (AnalysisTerminal terminal: resolution.getTerminals()) {
			final AnalysisArtifact terminalArtifact;
			if (terminal instanceof AnalysisArtifact) {
				terminalArtifact = (AnalysisArtifact) terminal;
			}
			else {
				terminalArtifact = ((AnalysisDependency)terminal).getSolution();
			}
			
			terminalArtifacts.add(map.computeIfAbsent(terminalArtifact, Artifact::from));
		}
		
		for (AnalysisArtifact solution: resolution.getSolutions()) {
			solutions.add(map.computeIfAbsent(solution, Artifact::from));
		}
		
		return artifactResolution;
	}
	
	private List<Artifact> asArtifacts(AnalysisArtifactResolution resolution) {
		List<Artifact> solutions = new ArrayList<>(resolution.getSolutions().size());
		
		for (AnalysisArtifact analysisArtifact: resolution.getSolutions()) {
			solutions.add(Artifact.from(analysisArtifact));
		}
		
		return solutions;
	}

}
