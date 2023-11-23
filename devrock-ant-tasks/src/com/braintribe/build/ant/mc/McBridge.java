package com.braintribe.build.ant.mc;

import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.tools.ant.BuildException;

import com.braintribe.devrock.mc.api.repository.CodebaseReflection;
import com.braintribe.devrock.mc.api.transitive.RangedTerminals;
import com.braintribe.devrock.model.repository.Repository;
import com.braintribe.devrock.model.repository.RepositoryConfiguration;
import com.braintribe.devrock.model.repository.filters.ArtifactFilter;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.ReasonException;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.compiled.CompiledArtifact;
import com.braintribe.model.artifact.compiled.CompiledArtifactIdentification;
import com.braintribe.model.artifact.compiled.CompiledDependencyIdentification;
import com.braintribe.model.artifact.compiled.CompiledPartIdentification;
import com.braintribe.model.artifact.compiled.CompiledTerminal;
import com.braintribe.model.artifact.consumable.Artifact;
import com.braintribe.model.artifact.consumable.ArtifactResolution;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.PartIdentification;

public interface McBridge extends AutoCloseable {
	/**
	 * reads ('compiles') the pom from file, ready to be processed
	 * @param pomFile - the {@link File} that contains the pom
	 * @return - the {@link CompiledArtifact}
	 */
	CompiledArtifact readArtifact( File pomFile);
	
	/**
	 * reads only the coordinates of the passed pom file 
	 * @param pomFile - the {@link File} that contains the pom
	 * @return - the extracted {@link CompiledArtifactIdentification}
	 */
	CompiledArtifactIdentification readArtifactIdentification(File pomFile);
	
	/**
	 * resolves a dependency to a matching artifact identification, if ranged, it will give you the highest version,
	 * will throw an {@link ReasonException} showing the error if it couldn't be resolved 
	 * @param dependency - the {@link CompiledDependencyIdentification}
	 * @return - the resulting {@link CompiledArtifactIdentification}
	 */
	CompiledArtifactIdentification resolveDependency(CompiledDependencyIdentification dependency);
	
	/**
	 * resolves a dependency to a matching artifact identification, if ranged, it will give you the highest version,
	 * wrapped as a {@link Maybe} reflecting the reason if failed
	 * @param dependency - the {@link CompiledDependencyIdentification}
	 * @return - a {@link Maybe} of {@link CompiledArtifactIdentification}
	 */
	Maybe<CompiledArtifactIdentification> resolveDependencyAsMaybe(CompiledDependencyIdentification dependency);
	
	/**
	 * resolves the artifact and enriches the parts passed, will throw an {@link ReasonException} if not found
	 * @param artifact - the {@link CompiledArtifactIdentification} that points to the artifact
	 * @param parts - a number of {@link PartIdentification}
	 * @return - the {@link Artifact}
	 */
	Artifact resolveArtifact(CompiledArtifactIdentification artifact, PartIdentification... parts);
	
	/**
	 * resolves the dependency to its best-matching artifact and enriches the parts passed, will throw
	 * an {@link ReasonException} if not found
	 * @param dependency - the {@link CompiledDependencyIdentification} 
	 * @param parts - a number of {@link PartIdentification}
	 * @return - the {@link Artifact
	 */
	Artifact resolveArtifact(CompiledDependencyIdentification dependency, PartIdentification... parts);
	
	/**
	 * resolves the artifact into a {@link CompiledArtifact} ready to be processed, will throw an {@link ReasonException} if not found
	 * @param artifact the {@link CompiledArtifactIdentification}
	 * @return - the {@link CompiledArtifact}
	 */
	CompiledArtifact resolveArtifact( CompiledArtifactIdentification artifact);
	
	/**
	 * resolves the {@link CompiledArtifactIdentification}
	 * @param artifact - the {@link CompiledArtifactIdentification}
	 * @return - a {@link Maybe} of the {@link CompiledArtifact}
	 */
	Maybe<CompiledArtifact> resolveArtifactAsMaybe( CompiledArtifactIdentification artifact);
	
	/**
	 * resolves all build dependencies for the passed terminals, using the filter
	 * @param rangedTerminals - the {@link RangedTerminals}
	 * @param artifactFilter - the {@link ArtifactFilter} to filter-out artifacts
	 * @return
	 */
	AnalysisArtifactResolution resolveBuildDependencies(RangedTerminals rangedTerminals, ArtifactFilter artifactFilter);
	
	/**
	 * @return - the {@link CodebaseReflection} (the repository that contains the sources)
	 */
	CodebaseReflection getCodebaseReflection();
	
	
	/**
	 * @return - the currently active {@link RepositoryConfiguration}
	 */
	RepositoryConfiguration getRepositoryConfiguration();
	
	/**
	 * @return - the {@link File} that points to the local repository 
	 */
	File getLocalRepository();
	
	/**
	 * @return - the {@link File} to dump the various analysis data 
	 */
	File getProcessingDataInsightFolder();
	
	/**
	 * @param repoId - the id (name) of the repository 
	 * @return - the {@link Repository} with the matching name if any
	 */
	Repository getRepository(String repoId);
	
	/**
	 * installs the artifact and reflects it with the {@link ArtifactResolution}
	 * @param artifact - the {@link Artifact}
	 * @return - the {@link ArtifactResolution}
	 */
	ArtifactResolution install(Artifact artifact);
	
	/**
	 * deploys an artifact to the repository passed and reflects it with {@link ArtifactResolution} 
	 * @param repository - the {@link Repository} that is the receiver
	 * @param artifact - the {@link Artifact}
	 * @return - the {@link ArtifactResolution}
	 */
	ArtifactResolution deploy(Repository repository, Artifact artifact);
	
	/**
	 * tests whether the passed repository contains an artifact with the passed coordinates
	 * @param repository - the {@link Repository} to test
	 * @param artifact - the {@link CompiledArtifactIdentification} of the artifact
	 * @return - true if it exists, false otherwise 
	 */
	boolean artifactExists(Repository repository, CompiledArtifactIdentification artifact);
	
	/**
	 * tests whether the passed repository contains an artifact with the part passed
	 * @param repository - the {@link Repository} to test
	 * @param part - {@link CompiledPartIdentification} 
	 * @return - true if it exists, false otherwise 
	 */
	boolean partExists(Repository repository, CompiledPartIdentification part);
	
	/**
	 * runs a classpath resolving
	 * @param terminals - an {@link Iterable} of {@link CompiledTerminal}
	 * @param scope - the scope to use (compile, runtime, test)
	 * @param tagRule - the tag-filter rule
	 * @param typeFilter - the type-filter rule
	 * @param parts - the {@link PartIdentification}s to enrich
	 * @param exclusions - global exlusions as a {@link Set} of {@link ArtifactIdentification}
	 * @return - an {@link AnalysisArtifactResolution} 
	 */
	AnalysisArtifactResolution resolveClasspath(Iterable<? extends CompiledTerminal> terminals, String scope,
			String tagRule, String typeFilter, List<PartIdentification> parts, Set<ArtifactIdentification> exclusions);
	
	/**
	 * runs a TDR with attached clash resolving. Intended to get a dependency list for change detection (solution hasher)
	 * @param terminals - an {@link Iterable} of {@link CompiledTerminal} 
	 * @return - an {@link AnalysisArtifactResolution} 
	 */
	public AnalysisArtifactResolution resolveClashfreeRelevantSolutions(Iterable<? extends CompiledTerminal> terminals, String scope, String tagRule, String typeRule, Set<ArtifactIdentification> exclusions);
	
	/**
	 * @param terminals - an {@link Iterable} of {@link CompiledTerminal}
	 * @param globalExclusions - global exlusions as a {@link List} of {@link ArtifactIdentification}
	 * @param excludedScopes - a {@link Set} of {@link String} with the scopes to exclude dependencies by
	 * @param includeOptional - whether to have optionals included
	 * @param enrichAllParts - whether to enrich all parts of the encountered artifacts
 	 * @return - the {@link AnalysisArtifactResolution}
	 */
	AnalysisArtifactResolution resolveBuildDependencies(Iterable<? extends CompiledTerminal> terminals, List<String> globalExclusions, Set<String> excludedScopes, boolean includeOptional, boolean enrichAllParts);
	
	/**
	 * @return - the {@link Repository} that is used as default upload target
	 */
	Repository getDefaultUploadRepository();
	
		
	/**
	 * dumps the currently active {@link RepositoryConfiguration} and produces a {@link BuildException}
	 * @param message - the message to convey
	 * @param cause - the causing {@link Exception} if any
	 * @return - a {@link BuildException} ready to throw
	 */
	BuildException produceContextualizedBuildException( String message, Exception cause);
		
	
	/**
	 * dumps the currently active {@link RepositoryConfiguration} and produces a {@link BuildException}
	 * @param message - the message to convey
	 * @return - the {@link BuildException}
	 */
	default BuildException produceContextualizedBuildException( String message) {
		return produceContextualizedBuildException(message, null);
	}
	
	/**
	 * writes the passed {@link AnalysisArtifactResolution} and {@link RepositoryConfiguration} to the configured problem-insight folder
	 * @param resolution - the {@link AnalysisArtifactResolution}
	 * @param configuration - the {@link RepositoryConfiguration}
	 */
	void writeResolutionAndConfigurationToProblemAnalysisFolder( AnalysisArtifactResolution resolution, RepositoryConfiguration configuration);
	
	/**
	 * writes the passed {@link AnalysisArtifactResolution} to the configured problem-insight folder
	 * @param resolution - the {@link AnalysisArtifactResolution}
	 */
	default void writeResolutionToProblemAnalysisFolder( AnalysisArtifactResolution resolution) {
		writeResolutionAndConfigurationToProblemAnalysisFolder( resolution, null);
	}
	
	/**
	 * writes the passed {@link RepositoryConfiguration} to the problem-insight folder
	 * @param configuration - the {@link RepositoryConfiguration}
	 */
	default void writeConfigurationToProblemAnalysisFolder( RepositoryConfiguration configuration) {
		writeResolutionAndConfigurationToProblemAnalysisFolder(null, configuration);
	}
	
	/**
	 * dumps the currently active {@link RepositoryConfiguration} and produces a {@link NoSuchElementException}
	 * @param message - the message to convey
	 * @param cause - the causing {@link Exception} if any
	 * @return - a {@link NoSuchElementException} ready to throw
	 */
	NoSuchElementException produceContextualizedNoSuchElementException( String message, Exception cause);
	
	/**
	 * dumps the currently active {@link RepositoryConfiguration} and produces a {@link NoSuchElementException}
	 * @param message - the message to convey
	 * @return - a {@link NoSuchElementException} ready to throw
	*/
	default NoSuchElementException produceContextualizedNoSuchElementException( String message) {
		return produceContextualizedNoSuchElementException(message, null);
	}
	
	/**
	 * writes the passed {@link AnalysisArtifactResolution} to the passed {@link File} as XML (stax, to digest big resolutions)
	 * @param resolution - the {@link AnalysisArtifactResolution}
	 * @param file - the {@link File} to write to
	 */
	void writeResolutionToFile( AnalysisArtifactResolution resolution, File file);
	
	@Override
	void close();
}
