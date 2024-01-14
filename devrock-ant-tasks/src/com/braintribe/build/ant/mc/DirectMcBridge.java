package com.braintribe.build.ant.mc;


import static com.braintribe.console.ConsoleOutputs.brightRed;
import static com.braintribe.console.ConsoleOutputs.sequence;
import static com.braintribe.console.ConsoleOutputs.text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.tools.ant.BuildException;

import com.braintribe.build.ant.mc.wire.AntMcCodebaseWireModule;
import com.braintribe.build.ant.mc.wire.AntMcWireModule;
import com.braintribe.build.ant.mc.wire.VeModule;
import com.braintribe.build.ant.utils.ArtifactResolutionUtil;
import com.braintribe.codec.marshaller.stax.StaxMarshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.mc.api.classpath.ClasspathResolutionContext;
import com.braintribe.devrock.mc.api.classpath.ClasspathResolutionContextBuilder;
import com.braintribe.devrock.mc.api.classpath.ClasspathResolutionScope;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.devrock.mc.api.deploy.ArtifactDeployer;
import com.braintribe.devrock.mc.api.download.PartEnrichingContext;
import com.braintribe.devrock.mc.api.download.PartEnrichingContextBuilder;
import com.braintribe.devrock.mc.api.repository.CodebaseReflection;
import com.braintribe.devrock.mc.api.repository.configuration.RepositoryReflection;
import com.braintribe.devrock.mc.api.resolver.ArtifactDataResolution;
import com.braintribe.devrock.mc.api.resolver.PartAvailabilityReflection;
import com.braintribe.devrock.mc.api.transitive.RangedTerminals;
import com.braintribe.devrock.mc.api.transitive.TransitiveDependencyResolver;
import com.braintribe.devrock.mc.api.transitive.TransitiveResolutionContext;
import com.braintribe.devrock.mc.api.transitive.TransitiveResolutionContextBuilder;
import com.braintribe.devrock.mc.core.commons.McReasonOutput;
import com.braintribe.devrock.mc.core.configuration.ArtifactIndexReader;
import com.braintribe.devrock.mc.core.declared.DeclaredArtifactIdentificationExtractor;
import com.braintribe.devrock.mc.core.filters.ArtifactFilterExpert;
import com.braintribe.devrock.mc.core.filters.ArtifactFilters;
import com.braintribe.devrock.mc.core.repository.index.ArtifactIndex;
import com.braintribe.devrock.mc.core.resolver.clashes.ClashResolver;
import com.braintribe.devrock.mc.core.resolver.common.AnalysisArtifactResolutionPreparation;
import com.braintribe.devrock.mc.core.resolver.rulefilter.BasicTagRuleFilter;
import com.braintribe.devrock.mc.core.resolver.rulefilter.BasicTypeRuleFilter;
import com.braintribe.devrock.mc.core.wirings.backend.contract.ArtifactDataBackendContract;
import com.braintribe.devrock.mc.core.wirings.classpath.contract.ClasspathResolverContract;
import com.braintribe.devrock.mc.core.wirings.devrock.contract.ProblemAnalysisContract;
import com.braintribe.devrock.mc.core.wirings.resolver.contract.ArtifactDataResolverContract;
import com.braintribe.devrock.mc.core.wirings.transitive.contract.TransitiveResolverContract;
import com.braintribe.devrock.model.mc.reason.UnresolvedDependency;
import com.braintribe.devrock.model.repository.CodebaseRepository;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.devrock.model.repository.filters.ArtifactFilter;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reason;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.gm.reason.TemplateReasons;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.analysis.AnalysisTerminal;
import com.braintribe.model.artifact.analysis.ClashResolvingStrategy;
import com.braintribe.model.artifact.analysis.DependencyClash;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledPartIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.consumable.PartEnrichment;
import com.braintribe.model.artifact.consumable.PartReflection;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;
import com.braintribe.utils.lcd.LazyInitialized;
import com.braintribe.ve.impl.OverridingEnvironment;
import com.braintribe.ve.impl.StandardEnvironment;
import com.braintribe.wire.api.Wire;
import com.braintribe.wire.api.context.WireContext;



public class DirectMcBridge implements McBridge {
	private static final String PROFILE_USECASE = "PROFILE_USECASE";
	private static final Comparator<AnalysisArtifact> solutionSortingComparator = Comparator.comparing(AnalysisArtifact::getArtifactId).thenComparing(Comparator.comparing(AnalysisArtifact::getGroupId));
	private static YamlMarshaller marshaller;
	static {
		marshaller = new YamlMarshaller();
		marshaller.setWritePooled(true);
	}
	@SuppressWarnings("unused")
	private static final StaxMarshaller staxMarshaller = new StaxMarshaller();
	private static DateFormat df = new SimpleDateFormat( "yyyy-MM-dd-HH-mm-ss");
	
	private WireContext<ClasspathResolverContract> classpathResolverContext;
	
	public DirectMcBridge(File devEnvFolder, File codebaseRoot, String codebasePattern) {
		classpathResolverContext = Wire.context( new AntMcCodebaseWireModule(devEnvFolder, codebaseRoot, codebasePattern));
	}
	
	public DirectMcBridge(File devEnvFolder, String profileUseCase, boolean ant) {
		AntMcWireModule antMcWireModule = new AntMcWireModule(devEnvFolder, ant);
		if (profileUseCase != null && !profileUseCase.isEmpty()) {
			OverridingEnvironment ve = new OverridingEnvironment(StandardEnvironment.INSTANCE);
			ve.setEnv(PROFILE_USECASE, profileUseCase);
			classpathResolverContext = Wire.context( antMcWireModule, new VeModule(ve));
		}
		else {
			classpathResolverContext = Wire.context( antMcWireModule);
		}
	}
	
	/**
	 * basically for debug purposes (and repolet in the background)
	 * @param settings - the settings.xml to use 
	 * @param port - the port where the repolet is answering
	 */
	public DirectMcBridge(File settings, int port) {
		AntMcWireModule antMcWireModule = new AntMcWireModule(null, false);
		OverridingEnvironment ve = new OverridingEnvironment(StandardEnvironment.INSTANCE);
		ve.setEnv("ARTIFACT_REPOSITORIES_EXCLUSIVE_SETTINGS", settings.getAbsolutePath());
		ve.setEnv( "port", Integer.toString( port));
		classpathResolverContext = Wire.context( antMcWireModule, new VeModule(ve));		
	}

	@Override
	public CompiledArtifact readArtifact(File pomFile) {
		Maybe<CompiledArtifact> maybe = getDataResolverContract().declaredArtifactCompiler().compileReasoned( pomFile);
		if (maybe.isSatisfied()) {
			return maybe.get();
		}
		else {
			throw new BuildException( "failed to read [" + pomFile.getAbsolutePath() + "] :" + maybe.whyUnsatisfied().stringify());
		}
	}
	
	@Override
	public CompiledArtifactIdentification readArtifactIdentification(File pomFile) {
		Maybe<CompiledArtifactIdentification> identificationPotential = DeclaredArtifactIdentificationExtractor.extractIdentification(pomFile);
		
		if (identificationPotential.isSatisfied())
			return identificationPotential.get();
		else
			throw new RuntimeException(identificationPotential.whyUnsatisfied().stringify());
	}

	@Override
	public AnalysisArtifactResolution resolveBuildDependencies(RangedTerminals rangedTerminals, ArtifactFilter artifactFilter) {
		
		ArtifactFilterExpert filterExpert = ArtifactFilters.forDenotation(artifactFilter);
		TransitiveResolutionContext resolutionContext = TransitiveResolutionContext.build() //
			.dependencyFilter(d -> filterExpert.matches(d)) //
			.includeImportDependencies(true) //
			.includeParentDependencies(true) //
			.includeRelocationDependencies(true) //
			.lenient(true) //
			.buildRange(rangedTerminals.range()) //
			.done();
		
		TransitiveDependencyResolver transitiveDependencyResolver = getTransitiveResolverContract().transitiveDependencyResolver();
		AnalysisArtifactResolution resolution = transitiveDependencyResolver.resolve(resolutionContext, rangedTerminals.terminals());
		
		// handle failure on repository configuration
		checkAndPropagateRepositoryConfigurationFailure(resolution);
		
		failOnFailedResolution(resolution);
		
		return resolution;
	}

	private void failOnFailedResolution(AnalysisArtifactResolution resolution) {
		if (resolution.hasFailed()) {
			AnalysisArtifactResolution trimmedResolution = ArtifactResolutionUtil.trimToFailures(resolution);

			ConsoleOutputs.println(sequence(
					brightRed("Error:\n"),
					new McReasonOutput().output(resolution.getFailure())
			));
						
			ConsoleOutputs.println("\nDependency paths to the errors\n");
			
			ArtifactResolutionUtil.printDependencyTree(trimmedResolution);
			
			writeResolutionAndConfigurationToProblemAnalysisFolder( resolution, getRepositoryConfiguration());

			//throw new BuildException("See errors above");
			throw produceContextualizedBuildException( "See errors above", null);
		}
	}
	
	@Override
	public CodebaseReflection getCodebaseReflection() {
		
		Repository repository = getRepositoryReflection().getRepository("codebase");

		ArtifactDataBackendContract dataBackendContract = getBackendContract();
		
		if (repository == null)
			return null;
		
		return (CodebaseReflection)dataBackendContract.codebaseRepository((CodebaseRepository)repository);
	}
	
	private RepositoryReflection getRepositoryReflection() {
		ArtifactDataResolverContract dataResolverContract = getDataResolverContract();		
		return dataResolverContract.repositoryReflection();
	}
	
	@Override
	public Artifact resolveArtifact(CompiledDependencyIdentification dependency, PartIdentification... parts) {
		Maybe<CompiledArtifactIdentification> potentialArtifact = getDataResolverContract().dependencyResolver().resolveDependency(dependency);
		
		if (potentialArtifact.isUnsatisfied()) {
			Reason reason = TemplateReasons.build(UnresolvedDependency.T) //
					.enrich(r -> r.setDependency(dependency)) //
					.cause(potentialArtifact.whyUnsatisfied()).toReason();
			
			throw produceContextualizedBuildExceptionReasoned(reason);
		}
		
		return resolveArtifact(potentialArtifact.get(), parts);
	}
	
	@Override
	public CompiledArtifact resolveArtifact(CompiledArtifactIdentification artifact) {
		Maybe<CompiledArtifact> maybe = resolveArtifactAsMaybe(artifact);
		if (maybe.isSatisfied())
			return maybe.get();
		
		throw produceContextualizedBuildException( maybe.whyUnsatisfied().stringify(), new ReasonException( maybe.whyUnsatisfied()));
	}
	
	@Override
	public Maybe<CompiledArtifact> resolveArtifactAsMaybe(CompiledArtifactIdentification artifact) {	
		return getDataResolverContract().redirectAwareCompiledArtifactResolver().resolve(artifact);
	}

	private ArtifactDataResolverContract getDataResolverContract() {
		return getTransitiveResolverContract().dataResolverContract();
	}

	private TransitiveResolverContract getTransitiveResolverContract() {
		return classpathResolverContext.contract().transitiveResolverContract();
	}
	
	private ProblemAnalysisContract getProblemAnalysisContract() {
		return classpathResolverContext.contract( ProblemAnalysisContract.class);
	}
	
	
	@Override
	public RepositoryConfiguration getRepositoryConfiguration() {
		return getDataResolverContract().repositoryReflection().getRepositoryConfiguration();
	}

	@Override
	public Repository getRepository(String repoId) {
		return getDataResolverContract().repositoryReflection().getRepository(repoId);
	}
	
	@Override
	public Repository getDefaultUploadRepository() {
		return getDataResolverContract().repositoryReflection().getUploadRepository();
	}
	
	@Override
	public CompiledArtifactIdentification resolveDependency(CompiledDependencyIdentification dependency) {
		return resolveDependencyAsMaybe(dependency).get();
	}	
		
	@Override
	public Maybe<CompiledArtifactIdentification> resolveDependencyAsMaybe(CompiledDependencyIdentification dependency) {	
		return getDataResolverContract().dependencyResolver().resolveDependency(dependency);
	}

	@Override
	public Artifact resolveArtifact(CompiledArtifactIdentification artifactIdentification, PartIdentification... parts) {
		CompiledArtifact compiledArtifact = getDataResolverContract().redirectAwareCompiledArtifactResolver().resolve(artifactIdentification).get();
		
		if (compiledArtifact.getInvalid()) {
			throw produceContextualizedBuildExceptionReasoned(compiledArtifact.getWhyInvalid());
		}
		
		AnalysisArtifact artifact = AnalysisArtifact.of(compiledArtifact);
		
		PartEnrichingContextBuilder contextBuilder = PartEnrichingContext.build();
		
		Stream.of(parts).forEach(contextBuilder::enrichPart);
		
		PartEnrichingContext enrichingContext = contextBuilder.done();
		
		getDataResolverContract().partEnricher().enrich(enrichingContext, artifact);
		
		if (artifact.hasFailed()) {
			throw produceContextualizedBuildExceptionReasoned(artifact.getFailure());
		}
		
		return artifact;
	}
	
	@Override
	public AnalysisArtifactResolution resolveClasspath(Iterable<? extends CompiledTerminal> terminals, String scope,
			String tagRule, String typeFilter, List<PartIdentification> parts, Set<ArtifactIdentification> exclusions) {
		ClasspathResolutionContextBuilder contextBuilder = ClasspathResolutionContext.build();

		// switch off default enriching, might be switched back on below 
		if (parts.size() == 0) {
			contextBuilder.enrichJar(false);
		}
		
		List<PartIdentification> genericPartIdentifications = new ArrayList<>();
		if (parts != null) {
			for (PartIdentification partIdentification: parts) {
				switch (partIdentification.asString()) {
				case ":jar":
					contextBuilder.enrichJar(true);
					break;
				case "javadoc:jar":
					contextBuilder.enrichJavadoc(true);
					break;
				case "sources:jar":
					contextBuilder.enrichSources(true);
					break;
				default:
					genericPartIdentifications.add(partIdentification);
					break;
				}
			}
			
			if (!genericPartIdentifications.isEmpty()) {
				PartEnrichingContextBuilder enrichingContextBuilder = PartEnrichingContext.build();
				genericPartIdentifications.forEach(enrichingContextBuilder::enrichPart);
				contextBuilder.enrich(enrichingContextBuilder.done());
			}
		}
		
		LazyInitialized<AnalysisArtifactResolution> preparationFailedResolution = new LazyInitialized<>(AnalysisArtifactResolution.T::create);
		// type filter expression -> filters every dependency 
		if (typeFilter != null) {
			Maybe<BasicTypeRuleFilter> typeRuleFilterPotential = BasicTypeRuleFilter.parse(typeFilter);
			
			if (typeRuleFilterPotential.isSatisfied()) {
				BasicTypeRuleFilter typeRuleFilter = typeRuleFilterPotential.get();
				contextBuilder.filterDependencies(typeRuleFilter);
			}
			else {
				AnalysisArtifactResolutionPreparation.acquireCollatorReason(preparationFailedResolution.get()) //
					.getReasons().add(typeRuleFilterPotential.whyUnsatisfied());
			}
		}
		
		String effectiveScope = Optional.ofNullable(scope).orElse("runtime");
		
		contextBuilder.scope(ClasspathResolutionScope.valueOf(effectiveScope));
		contextBuilder.lenient(true);
		
		if (exclusions != null)
			contextBuilder.globalExclusions(exclusions);
		// tag rule expression 	
		if (tagRule != null) {
			Maybe<BasicTagRuleFilter> ruleFilterPotential = BasicTagRuleFilter.parse(tagRule);
			
			if (ruleFilterPotential.isSatisfied()) {
				BasicTagRuleFilter tagRuleFilter = ruleFilterPotential.get();
				contextBuilder.dependencyPathFilter(tagRuleFilter);
			}
			else {
				AnalysisArtifactResolutionPreparation.acquireCollatorReason(preparationFailedResolution.get()) //
					.getReasons().add(ruleFilterPotential.whyUnsatisfied());
			}
		}
		
		// return eagerly with preparation failure reasons
		if (preparationFailedResolution.isInitialized())
			return preparationFailedResolution.get();
		
		ClasspathResolutionContext resolutionContext = contextBuilder.done();
		AnalysisArtifactResolution resolution = classpathResolverContext.contract().classpathResolver().resolve(resolutionContext, terminals);
		
		// handle configuration failure here
		checkAndPropagateRepositoryConfigurationFailure(resolution);
		
		failOnFailedResolution(resolution);
		
		return resolution;	
	}
	
	private List<Reason> getRootCauses(Reason failure) {
		Set<Reason> visited = new HashSet<>();
		List<Reason> causes = new ArrayList<>();
		
		scanForRootCauses(failure, causes, visited);
		return causes;
	}

	private void scanForRootCauses(Reason reason, List<Reason> causes, Set<Reason> visited) {
		if (!visited.add(reason))
			return;
		
		if (reason.getReasons().isEmpty()) {
			causes.add(reason);
			return;
		}
		
		for (Reason cause: reason.getReasons()) {
			scanForRootCauses(cause, causes, visited);
		}
	}
	
	@Override
	public AnalysisArtifactResolution resolveClashfreeRelevantSolutions(Iterable<? extends CompiledTerminal> terminals, String scope, String tagRule, String typeRule, Set<ArtifactIdentification> exclusions) {
		//
		// build transitive context
		// 
		
		TransitiveResolutionContextBuilder transitiveResolutionContextBuilder = TransitiveResolutionContext.build();
		
		// make sure to include all relevant artifacts, i.e. also parents, imports and relocations
		// CURRENTLY: NO HASHING FOR PARENTS/IMPORTS/RELOCATIONS
		transitiveResolutionContextBuilder.includeImportDependencies(false);
		transitiveResolutionContextBuilder.includeParentDependencies(false);
		transitiveResolutionContextBuilder.includeRelocationDependencies( false);
		
		
		// just make sure that the normal deps are selected 
		transitiveResolutionContextBuilder.includeStandardDependencies(true);


		//
		// filters
		//
		
		// type rule -> dependency filter
		Predicate<AnalysisDependency> dependencyFilterPerTypeRule = null;		
		if (typeRule != null) {
			Maybe<BasicTypeRuleFilter> typeRuleFilterPotential = BasicTypeRuleFilter.parse(typeRule);
			
			if (typeRuleFilterPotential.isSatisfied()) {
				BasicTypeRuleFilter typeRuleFilter = typeRuleFilterPotential.get();
				dependencyFilterPerTypeRule = typeRuleFilter;
			}
			else {
				throw produceContextualizedBuildException("cannot process type rule [" + typeRule + "]", null);
			}
		}
		// scope as a rule 
		String effectiveScope = Optional.ofNullable(scope).orElse("compile");
		Predicate<AnalysisDependency> dependencyFilterPerScope = new Predicate<AnalysisDependency>() {

			@Override
			public boolean test(AnalysisDependency t) {
				if (t.getScope() == null || t.getScope().equals(effectiveScope)) {
					return true;
				}
				return false;
			}
			
		}; 
		// combine both dependency filters 
		if (dependencyFilterPerTypeRule != null) {
			transitiveResolutionContextBuilder.dependencyFilter( dependencyFilterPerScope.and(dependencyFilterPerTypeRule));
		}
		else {
			transitiveResolutionContextBuilder.dependencyFilter( dependencyFilterPerScope);	
		}
								
		// tag rule -> dependency path filter
		BasicTagRuleFilter tagRuleFilter = null;
		if (tagRule != null) {
			Maybe<BasicTagRuleFilter> ruleFilterPotential = BasicTagRuleFilter.parse(tagRule);
			
			if (ruleFilterPotential.isSatisfied()) {
				tagRuleFilter = ruleFilterPotential.get();
			}
			else {
				throw produceContextualizedBuildException("cannot process tag rule [" + tagRule + "]", null);
			}
		}
		
		// dependency path filter to catch 'optional' and - as an afterthought - scope combinations
		OptionalDependencyFilter scopeAndOptionalFilter = new OptionalDependencyFilter( effectiveScope);
		
		// if a tag rule filter exists, combine the filters
		if (tagRuleFilter != null) {
			transitiveResolutionContextBuilder.dependencyPathFilter(scopeAndOptionalFilter.and(tagRuleFilter));
		}
		else {
			transitiveResolutionContextBuilder.dependencyPathFilter(scopeAndOptionalFilter);	
		}
				
		// exclusions
		if (exclusions != null)
			transitiveResolutionContextBuilder.globalExclusions(exclusions);
		
		TransitiveResolutionContext transitiveResolutionContext = transitiveResolutionContextBuilder.done();
		
		//
		// process 
		//
		
		
		// get all involved artifacts from the transitive dependency resolver		
		AnalysisArtifactResolution resolution = getTransitiveResolverContract().transitiveDependencyResolver().resolve(transitiveResolutionContext, terminals);
		
		// handle configuration errors 
		
		
		// get the entry points of the dependency tree returned to run clashes over it
		List<AnalysisTerminal> analysisTerminals = resolution.getTerminals();
		List<AnalysisDependency> dependencies = new ArrayList<>();
		
		for (AnalysisTerminal terminal: analysisTerminals) {
			if (terminal instanceof AnalysisArtifact) {
				AnalysisArtifact artifact = (AnalysisArtifact)terminal;
				dependencies.addAll(artifact.getDependencies());
			}
			else if (terminal instanceof AnalysisDependency) {
				AnalysisDependency dependency = (AnalysisDependency)terminal;
				dependencies.add(dependency);
			}
		}
		
		// resolve clashes
		List<DependencyClash> dependencyClashes = ClashResolver.resolve(dependencies, ClashResolvingStrategy.highestVersion);
		resolution.setClashes(dependencyClashes);
		
		AnalysisArtifactResolutionPreparation analysisArtifactResolutionPreparation = new AnalysisArtifactResolutionPreparation(resolution, solutionSortingComparator, (s) -> true);
		analysisArtifactResolutionPreparation.process();
				
		// handle failure on repository configuration
		checkAndPropagateRepositoryConfigurationFailure(resolution);
		return resolution;
		
	}

	

	@Override
	public AnalysisArtifactResolution resolveBuildDependencies(Iterable<? extends CompiledTerminal> terminals,
			List<String> globalExclusions, Set<String> excludedScopes, boolean includeOptional,
			boolean enrichAllParts) {

		
		List<Pattern> patterns = new ArrayList<>(globalExclusions.size());
		
		for (String patternStr : globalExclusions) {
			patterns.add(Pattern.compile(patternStr));
		}
		
		Predicate<AnalysisArtifact> artifactFilter = a -> {
			for (Pattern p: patterns) {
				String artifactId = a.asString();
				if (p.matcher(artifactId).matches()) {
					ConsoleOutputs.println("Excluding artifact "+artifactId+" because it is globally excluded.");
					return false;
				}
			}
			
			return true;
		};
		
		Predicate<AnalysisDependency> dependencyFilter = d -> {
			if (excludedScopes.contains(d.getScope()))
				return false;
			
			if (!includeOptional && d.getOptional())
				return false;
			
			return true;
		};
		
		PartAvailabilityReflection partAvailabilityReflection = getDataResolverContract().partAvailabilityReflection();
		
		PartEnrichingContext enrichingContext = PartEnrichingContext.build().enrichingExpert(a -> {
			List<PartReflection> parts = partAvailabilityReflection.getAvailablePartsOf(a.getOrigin());
			return parts.stream().map(this::buildEnrichment).collect(Collectors.toList());
		}).done();
		
		TransitiveResolutionContext resolutionContext = TransitiveResolutionContext.build() //
			.artifactFilter(artifactFilter) //
			.dependencyFilter(dependencyFilter) //
			.enrich(enrichingContext) // 
			.done();

		AnalysisArtifactResolution resolution = getTransitiveResolverContract().transitiveDependencyResolver().resolve(resolutionContext, terminals);

		// handle failure on repository configuration
		checkAndPropagateRepositoryConfigurationFailure(resolution);
		
		return resolution;
	}

	private PartEnrichment buildEnrichment(PartReflection p) {
		PartEnrichment enrichment = PartEnrichment.T.create();
		enrichment.setClassifier(p.getClassifier());
		enrichment.setType(p.getType());
		enrichment.setMandatory(true);
		return enrichment;
	}
	
	@Override
	public File getLocalRepository() {
		return getDataResolverContract().localRepositoryRoot();
	}
	
	

	@Override
	public File getProcessingDataInsightFolder() {	
		return getProblemAnalysisContract().processingDataInsightFolder();
	}

	@Override
	public ArtifactResolution deploy(Repository repository, Artifact artifact) {
		ArtifactDataBackendContract backendContract = getBackendContract();
		
		ArtifactDeployer artifactDeployer = backendContract.artifactDeployer(repository);
		return artifactDeployer.deploy(artifact);
	}
	
	@Override
	public ArtifactResolution install(Artifact artifact) {
		
		RepositoryReflection repositoryReflection = getDataResolverContract().repositoryReflection();
		Repository repository = repositoryReflection.getRepository("local");
		
		if (repository == null) {
			throw produceContextualizedNoSuchElementException( "Missing local repository in configuration");
		}

		return deploy(repository, artifact);
	}

	@Override
	public boolean artifactExists(Repository repository, CompiledArtifactIdentification artifact) {
		return partExists(repository, CompiledPartIdentification.from(artifact, PartIdentifications.pom));
	}
	
	@Override
	public boolean partExists(Repository repository, CompiledPartIdentification part) {
		Maybe<ArtifactDataResolution> partResolution = getBackendContract().repository(repository).resolvePart(part, part);
		
		if (partResolution.isUnsatisfiedBy(NotFound.T))
			return false;
		
		return partResolution.get().isBacked();
	}
	
	@Override
	public Maybe<ArtifactIndex> readLatestArtifactIndex(Repository repository) {
		ArtifactDataBackendContract backendContract = getBackendContract();

		ArtifactIndexReader reader = new ArtifactIndexReader();
		reader.setArtifactDataResolverFactory(backendContract::repository);

		return reader.readFullArtifactIndex(repository);
	}

	private ArtifactDataBackendContract getBackendContract() {
		return classpathResolverContext.contract(ArtifactDataBackendContract.class);
	}
	
	public BuildException produceContextualizedBuildExceptionReasoned(Reason reason) {
		ConsoleOutputs.println(sequence(
				brightRed("Error: "),
				text(reason.stringify())
		));
		throw produceContextualizedBuildException("Error: see output above");
	}
	
	@Override
	public BuildException produceContextualizedBuildException(String message, Exception cause) {
		RepositoryConfiguration repositoryConfiguration = getRepositoryConfiguration();
		writeConfigurationToProblemAnalysisFolder(repositoryConfiguration);
		return new BuildException(message, cause);		
	}

	@Override
	public NoSuchElementException produceContextualizedNoSuchElementException(String message, Exception cause) {
		RepositoryConfiguration repositoryConfiguration = getRepositoryConfiguration();
		writeConfigurationToProblemAnalysisFolder(repositoryConfiguration);
		//return new NoSuchElementException( message, cause);
		return new NoSuchElementException( message);
	}
		
	/**
	 * if the current {@link RepositoryConfiguration} has failed, its failure reason is attach to the resolution (which is failed then if not already)
	 * @param resolution - the resolution to attach the failure of the current repository configuration
	 */
	private void checkAndPropagateRepositoryConfigurationFailure(AnalysisArtifactResolution resolution) {
		RepositoryConfiguration repositoryConfiguration = getRepositoryConfiguration();
		if (repositoryConfiguration.hasFailed()) {
			Reason configurationFailureReason = repositoryConfiguration.getFailure();
			Reason resolutionFailureReason = resolution.getFailure();
			if (resolutionFailureReason == null) {
				resolution.setFailure( configurationFailureReason);
			}
			else {
				resolutionFailureReason.getReasons().add(configurationFailureReason);
			}
		}
	}
	
	
	
	
	@Override
	public void writeResolutionAndConfigurationToProblemAnalysisFolder(AnalysisArtifactResolution resolution, RepositoryConfiguration configuration) {
		File problemAnalysisFolder = getProcessingDataInsightFolder();
		Date now = new Date();
		if (resolution != null) {
			dumpResolution( problemAnalysisFolder, resolution, now);
		}
		if (configuration != null) {
			dumpConfiguration( problemAnalysisFolder, configuration, now);
		}
				
	}

	private void dumpConfiguration(File folder, RepositoryConfiguration repositoryConfiguration, Date timestamp) {		
		String timestampAsString = df.format( timestamp);
		File yamlFile = new File( folder, "repository-configuration-dump-" + timestampAsString + ".yaml");
		try (OutputStream out = new FileOutputStream(yamlFile)) {
			marshaller.marshall(out, repositoryConfiguration);
		} catch (Exception e) {			
			System.err.println("cannot dump repository configuration as " + e.getMessage());
		}
		
	}

	private void dumpResolution(File folder, AnalysisArtifactResolution resolution, Date timestamp) {
		String timestampAsString = df.format( timestamp);
		File file = new File( folder, "artifact-resolution-dump-" + timestampAsString + ".yaml");
		writeResolutionToFile(resolution, file);		
	}
	

	@Override
	public void writeResolutionToFile(AnalysisArtifactResolution resolution, File file) {
		try (OutputStream out = new FileOutputStream(file)) {
			marshaller.marshall(out, resolution);
		} catch (Exception e) {			
			System.err.println("cannot dump artifact resolution as " + e.getMessage());
		}		
	}

	@Override
	public void close() {
		classpathResolverContext.close();
	}
	
}
