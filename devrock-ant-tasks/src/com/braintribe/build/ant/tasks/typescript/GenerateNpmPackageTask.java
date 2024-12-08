// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript;

import static com.braintribe.model.typescript.TypeScriptWriterHelper.createCustomGmTypeFilter;
import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.concat;
import static com.braintribe.utils.lcd.CollectionTools2.newSet;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.partitioningBy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.codec.Charsets;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.utils.DrAntTools;
import com.braintribe.build.process.ProcessExecution;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.UnknownEnumException;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.consumable.Part;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.generic.mdec.ModelDeclaration;
import com.braintribe.model.jvm.reflection.ModelDeclarationParser;
import com.braintribe.model.meta.GmType;
import com.braintribe.model.processing.itw.analysis.JavaTypeAnalysis;
import com.braintribe.model.resource.FileResource;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.typescript.ModelEnsuringContext;
import com.braintribe.model.typescript.ModelEnsuringDTsWriter;
import com.braintribe.model.typescript.ModelEnsuringJsWriter;
import com.braintribe.model.typescript.TypeScriptWriterForClasses;
import com.braintribe.model.typescript.TypeScriptWriterForModels;
import com.braintribe.model.typescript.TypeScriptWriterHelper;
import com.braintribe.model.typescript.TypeScriptWriterHelper.NpmPackageScopedName;
import com.braintribe.model.version.Version;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.CollectionTools2;
import com.braintribe.utils.lcd.NullSafe;

/**
 * 
 * <h2>Configuring NPM repo URL for given artifact</h2>
 * 
 * To configure the npm repository URL for given artifact set property "npmRegistryUrl" in its pom.xml.
 * <p>
 * <b>npmjs.com:</b> DON'T SET, THAT'S THE DEFAULT<br>
 * <b>GitHub:</b> https://npm.pkg.github.com
 * 
 * @author peter.gazdik
 */
public class GenerateNpmPackageTask extends Task {

	private static final Logger log = Logger.getLogger(GenerateNpmPackageTask.class);

	private static final String CLASS_FILE_SUFFIX = ".class";
	private static final int CLASS_FILE_SUFFIX_LENGTH = CLASS_FILE_SUFFIX.length();

	private String npmPackagingStr;
	private File buildFolder;
	private File outputDir;
	private String resolutionId;
	private String npmPackageName;

	// @formatter:off
	// doesn't exist for GWT terminals, but we use it to get the parent and navigate to possible static .d.ts files
	@Required public void setNpmPackaging(String npmPackaging) { this.npmPackagingStr = npmPackaging; }
	@Required public void setBuildFolder(File buildFolder) { this.buildFolder = buildFolder; }
	public void setResolutionId(String resolutionId) { this.resolutionId = resolutionId; }

	/** Optional, defaults to artifactId.
	 * For GWT terminals, the value for pretty build is "${artifactId}-dev. */
	public void setNpmPackageName(String npmPackageName) { this.npmPackageName = npmPackageName;}
	// @formatter:on

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@Override
	public void execute() throws BuildException {
		DrAntTools.runAndPrintStacktraceIfNonBuildException(this::_execute);
	}

	private void _execute() throws BuildException {
		try {
			new GenerateNpmPackageExecution().run();

		} catch (IOException e) {
			throw new BuildException("Error while generating NPM package.", e);
		}
	}

	private class GenerateNpmPackageExecution {

		private static final String GM_CORE_API_VERSIONLESS = "com.braintribe.gm:gm-core-api";

		private final AnalysisArtifactResolution resolution = getProject()
				.getReference(resolutionId != null ? resolutionId : "LAST-DEPENDENCIES-RESOLUTION");
		private final List<AnalysisArtifact> solutions = resolution.getSolutions();
		private final ClassLoader classLoader = TsClassLoaderFactory.prepareClassLoader(buildFolder, solutions);
		private final Function<Class<?>, String> jsNameResolver = TypeScriptWriterHelper.jsNameResolver(classLoader);
		private final Predicate<Class<?>> customGmTypeFilter = createCustomGmTypeFilter(classLoader);
		private final Set<String> modelArtifactNames = resolveModelArtifactNames();

		// Current Artifact
		private final AnalysisArtifact currentArtifact = resolveCurrentProject();
		private final String npmRegistryUrl = resolveCurrentArtifactNpmRegistryUrl();
		private final NpmPackageScopedName npmPackageScopedName = resolveNpmPackageScopedName();

		private final String gId = currentArtifact.getGroupId();
		private final String aId = currentArtifact.getArtifactId();
		private final String version = currentArtifact.getVersion();

		private final File outputSrcDir = new File(outputDir, "dist");

		private List<AnalysisArtifact> deps = emptyList();

		private NpmPackaging npmPackaging;
		private List<Class<?>> regularClasses = emptyList();
		private List<Class<?>> gmClasses;
		private List<Class<?>> gmClassesForwarded;
		private List<GmType> gmTypesDeclared;
		private List<GmType> gmTypesForwarded;
		private List<GmType> gmTypesAll;

		private String problemMsg;

		private Writer dtsWriter;
		private Writer jsWriter;

		private Set<String> resolveModelArtifactNames() {
			Set<String> result = asSet();

			for (AnalysisArtifact s : solutions)
				if (isMarkedAsModel(s))
					result.add(versionlessName(s));

			return result;
		}

		private AnalysisArtifact resolveCurrentProject() {
			return (AnalysisArtifact) resolution.getTerminals().get(0);
		}

		private String resolveCurrentArtifactNpmRegistryUrl() {
			try {
				return currentArtifact.getOrigin().getProperties().get("npmRegistryUrl");
			} catch (NullPointerException e) {
				log.warn("Error while resolving npmRepoUrl property from pom.xml", e);
				return null;
			}
		}

		private NpmPackageScopedName resolveNpmPackageScopedName() {
			String groupId = currentArtifact.getGroupId();
			String packageSuffix = NullSafe.get(npmPackageName, currentArtifact.getArtifactId());

			return TypeScriptWriterHelper.npmPackageFullName(groupId, packageSuffix);
		}

		// ################################################
		// ##. . . . . . . . . . RUN . . . . . . . . . . ##
		// ################################################

		public void run() throws IOException {
			determineNpmPackaging();

			validate();

			writeNpmPackage();
		}

		// ################################################
		// ##. . . . NPM packaging determination . . . . ##
		// ################################################

		private void determineNpmPackaging() {
			npmPackaging = determineNpmPackagingHelper();
		}

		private NpmPackaging determineNpmPackagingHelper() {
			if (npmPackagingStr != null)
				return NpmPackaging.valueOf(npmPackagingStr);

			if (isMarkedAsModel(currentArtifact))
				return NpmPackaging.model;

			String npmPackaging = getNpmPackagingOf(currentArtifact);
			if (npmPackaging == null)
				throw new BuildException("Cannot generate NPM package. This artifact is not a model and no 'npmPackaging' property "
						+ "found in its pom.xml. Possible values: '" + NpmPackaging.typeScriptDeclaration + "', '" + NpmPackaging.gwtTerminal + "'");

			switch (npmPackaging) {
				case "typeScriptDeclaration":
					return NpmPackaging.typeScriptDeclaration;
				case "gwtTerminal":
					return NpmPackaging.gwtTerminal;
				default:
					throw new BuildException("Cannot generate NPM package. Invalid value for 'npmPackaging' property: " + npmPackaging
							+ ". Possible values: '" + NpmPackaging.typeScriptDeclaration + "', '" + NpmPackaging.gwtTerminal + "'");
			}
		}

		// ################################################
		// ## . . . . . . . . Validation . . . . . . . . ##
		// ################################################

		private void validate() {
			if (findProblem())
				throw new BuildException(problemMsg);
		}

		private boolean findProblem() {
			if (npmPackaging == NpmPackaging.gwtTerminal)
				return false;

			return isProblem(buildFolder == null, "buildFolder not specified")
					|| isProblem(!buildFolder.isDirectory(), "buildFolder is not an existing directory: " + buildFolder.getAbsolutePath());
		}

		private boolean isProblem(boolean test, String msg) {
			problemMsg = msg;
			return test;
		}

		// ################################################
		// ## . . . . . . Write NPM package . . . . . . .##
		// ################################################

		private void writeNpmPackage() throws IOException {
			FileTools.ensureFolderExists(outputSrcDir);

			switch (npmPackaging) {
				case gwtTerminal:
					writeGwtTerminalPackage();
					break;
				case model:
					writeModelPackage();
					break;
				case typeScriptDeclaration:
					writeTypeScriptDeclarationPackage();
					break;
				default:
					throw new UnknownEnumException(npmPackaging);
			}

			closeWriters();

			writePackageJson();
			writeNpmrc();
		}

		private void writeGwtTerminalPackage() throws IOException {
			resolveDepsAllModels();

			addClassesFromDepsToRegularClasses();

			writeGwtTerminalDts();

			addJsinteropToDts();
		}

		private void writeModelPackage() throws IOException {
			resolveDepsDirectModelsOnly();

			analyzeLocalClasses();

			writeModelEnsuringJsAndDTs();
		}

		private void writeTypeScriptDeclarationPackage() throws IOException {
			if (isCurrentGmCoreApi())
				// no resolveDeps needed --> gm-core-api doesn't depend on any model or dts artifact
				analyzeLocalClasses();
			else
				resolveDepsAllModels();

			addClassesFromDepsToRegularClasses();

			// we only expect model types in gm-core-api, and we don't ensure them
			if (isCurrentGmCoreApi())
				writeModelEnsuringJsAndDTs();
			else
				writeMetaExportingJsAndDts();

			addJsinteropToDts();
		}

		// ################################################
		// ##. . . . . Dependencies resolution . . . . . ##
		// ################################################

		private void resolveDepsDirectModelsOnly() {
			resolveDepsWithModels(listDirectModelDeps());
		}

		private List<AnalysisArtifact> listDirectModelDeps() {
			return currentArtifact.getDependencies().stream() //
					.filter(this::isModel) //
					.map(AnalysisDependency::getSolution) //
					.collect(Collectors.toList());
		}

		private void resolveDepsAllModels() {
			resolveDepsWithModels(listAllModelDeps());
		}

		private List<AnalysisArtifact> listAllModelDeps() {
			return filterDeps(this::isModel);
		}

		private void resolveDepsWithModels(List<AnalysisArtifact> models) {
			deps = CollectionTools2.concat(models, resolveAllDtsDeps());
		}

		private List<AnalysisArtifact> resolveAllDtsDeps() {
			return filterDeps(this::isTypeScriptDeclaration);
		}

		private List<AnalysisArtifact> filterDeps(Predicate<AnalysisArtifact> predicate) {
			return solutions.stream() //
					.filter(predicate) //
					.filter(aa -> !aa.getDependers().isEmpty()) //
					.map(aa -> aa.getDependers().iterator().next()) //
					.map(AnalysisDependency::getSolution) //
					.collect(Collectors.toList());
		}

		// ################################################
		// ## . . . . . Analyze Local Classes . . . . . .##
		// ################################################

		private void analyzeLocalClasses() {
			JavaTypeAnalysis jta = new JavaTypeAnalysis();
			jta.setClassLoader(classLoader);
			jta.setRequireEnumBase(rootModelMajor() >= 2);

			Map<Boolean, List<Class<?>>> classes = localGmAndRegularClasses();

			regularClasses = classes.get(FALSE);
			gmClasses = classes.get(TRUE);
			gmClassesForwarded = findForwardedGmClasses();
			gmTypesDeclared = extractGmTypes(jta, gmClasses);
			gmTypesForwarded = extractGmTypes(jta, gmClassesForwarded);
			gmTypesAll = allGmTypes();

			if (!regularClasses.isEmpty())
				log("Regular classes found: " + regularClasses.size());

			if (!gmClasses.isEmpty())
				log("Model classes found: " + gmClasses.size());

			if (!gmClasses.isEmpty())
				log("Model classes (forwarded) found: " + gmClassesForwarded.size());
		}

		private int rootModelMajor() {
			return solutions.stream() //
					.filter(ds -> "com.braintribe.gm".equals(ds.getGroupId()) && "root-model".equals(ds.getArtifactId())) //
					.mapToInt(ds -> Version.parse(ds.getVersion()).getMajor()) //
					.findFirst() //
					.orElse(1);
		}

		// Result is partitioned based on given class being a GM type (enum or entity) or a regular java class
		private Map<Boolean, List<Class<?>>> localGmAndRegularClasses() {
			Path buildFolderPath = buildFolder.toPath().toAbsolutePath();

			try {
				return Files.walk(buildFolderPath) //
						.filter(this::isClassFile) //
						.map(buildFolderPath::relativize) //
						.map(this::toClassName) //
						.map(this::toClassIfPossible) //
						.filter(c -> c != null) //
						.collect(partitioningBy(customGmTypeFilter));

			} catch (IOException e) {
				throw new BuildException("Error while looking for class files in folder: " + buildFolderPath, e);
			}
		}

		private boolean isClassFile(Path path) {
			return Files.isRegularFile(path) && path.toString().endsWith(CLASS_FILE_SUFFIX);
		}

		private String toClassName(Path relativeClassFilePath) {
			String s = relativeClassFilePath.toString();
			s = StringTools.removeLastNCharacters(s, CLASS_FILE_SUFFIX_LENGTH);
			s = s.replace(File.separatorChar, '.');

			return s;
		}

		private List<Class<?>> findForwardedGmClasses() {
			if (!isMarkedAsModel(currentArtifact))
				return emptyList();

			Set<String> declaredTypeSignatures = readTypeSignaturesFromModelDeclarationXml();
			for (Class<?> clazz : gmClasses)
				declaredTypeSignatures.remove(clazz.getName());

			return declaredTypeSignatures.stream() //
					.map(this::toClassIfPossible) //
					.filter(c -> c != null) //
					// just in case the xml contains some BS, e.g. classes with @GmSystemInterface
					.filter(customGmTypeFilter) //
					.collect(Collectors.toList());
		}

		private Set<String> readTypeSignaturesFromModelDeclarationXml() {
			File file = new File(buildFolder, "model-declaration.xml");
			if (!file.exists()) {
				log("Will not include forwarded types, model-declaration.xml not found: " + file.getAbsolutePath(), Project.MSG_WARN);
				return emptySet();
			}

			ModelDeclaration modelDeclaration = FileTools.read(file).fromInputStream(ModelDeclarationParser::parse);
			return modelDeclaration.getTypes();
		}

		private static List<GmType> extractGmTypes(JavaTypeAnalysis jta, List<Class<?>> classes) {
			return classes.stream() //
					.map(jta::getGmTypeUnchecked) //
					.collect(Collectors.toList());
		}

		private List<GmType> allGmTypes() {
			return CollectionTools2.concat(gmTypesDeclared, gmTypesForwarded);
		}

		// ################################################
		// ##. . . . . . Analyze Classpath . . . . . . . ##
		// ################################################

		private void addClassesFromDepsToRegularClasses() {
			Set<String> classNames = findJsinteropArtifactsClasses();

			Map<Boolean, List<Class<?>>> classes = classNames.stream() //
					.map(this::toClassIfPossible) //
					.filter(c -> c != null) //
					.collect(partitioningBy(customGmTypeFilter));

			List<Class<?>> dependedRegularClasses = classes.get(FALSE);
			regularClasses = concat(regularClasses, dependedRegularClasses);

			log("Regular classes (including dependencies) found: " + regularClasses.size());
		}

		private Set<String> findJsinteropArtifactsClasses() {
			final String DOT_CLASS = ".class";
			final int DOT_CLASS_LENGTH = DOT_CLASS.length();

			Set<AnalysisArtifact> excludedDeps = transitiveClosure(deps);

			Set<String> result = newSet();

			for (AnalysisArtifact artifact : solutions) {
				if (isModel(artifact) || excludedDeps.contains(artifact))
					continue;

				if (!isJsinterop(artifact))
					continue;

				Part part = artifact.getParts().get(PartIdentifications.jar.asString());
				if (part == null) {
					log("Could not find part 'jar' in " + artifact.getArtifactId(), Project.MSG_WARN);
					continue;
				}

				Resource resource = part.getResource();
				if (resource instanceof FileResource f) {
					try (JarFile jarFile = new JarFile(new File(f.getPath()))) {

						Iterable<JarEntry> iterable = jarFile.entries()::asIterator;

						for (JarEntry entry : iterable) {
							// path is something like "my/pckg/MyClass.class"
							String path = entry.getName();
							if (!path.endsWith(DOT_CLASS))
								continue;

							String className = StringTools.removeLastNCharacters(path, DOT_CLASS_LENGTH).replace('/', '.');
							result.add(className);
						}

					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}

			}

			return result;
		}

		private Set<AnalysisArtifact> transitiveClosure(List<AnalysisArtifact> artifacts) {
			Set<AnalysisArtifact> result = newSet();

			findTransitiveClosure(artifacts, result);

			return result;
		}

		private void findTransitiveClosure(List<AnalysisArtifact> artifacts, Set<AnalysisArtifact> result) {
			for (AnalysisArtifact artifact : artifacts)
				if (result.add(artifact))
					findTransitiveClosure(depSolutions(artifact), result);
		}

		private List<AnalysisArtifact> depSolutions(AnalysisArtifact artifact) {
			return artifact.getDependencies().stream() //
					.map(AnalysisDependency::getSolution) //
					.collect(Collectors.toList());
		}

		private Class<?> toClassIfPossible(String className) {
			try {
				return Class.forName(className, false, classLoader);

			} catch (ClassNotFoundException e) {
				log("Class not found and will be ignored from TypeScript generation: " + className);
				return null;
			} catch (LinkageError e) {
				return null;
			}
		}

		// ################################################
		// ##. . . . . . . . Write Files . . . . . . . . ##
		// ################################################

		private void addJsinteropToDts() throws IOException {
			Writer writer = dtsWriter();

			writeBlockComment(writer, "JsInterop");

			TypeScriptWriterForClasses.write(regularClasses, customGmTypeFilter, writer);
		}

		private void writeModelEnsuringJsAndDTs() throws IOException {
			ModelEnsuringContext meContext = ModelEnsuringContext.createForNpm(gmTypesAll, gId, aId, version, deps);

			writeModelEnsuringDTs(meContext);

			ModelEnsuringJsWriter.writeJs(meContext, jsWriter(), isCurrentGmCoreApi());
		}

		private void writeModelEnsuringDTs(ModelEnsuringContext meContext) throws IOException {
			Writer writer = dtsWriter();

			writeBlockComment(writer, "Types");

			ModelEnsuringDTsWriter.writeDts(meContext, writer);
			TypeScriptWriterForModels.write(gmTypesDeclared, jsNameResolver, writer);

			if (isCurrentGmCoreApi())
				copyStaticDts(writer);
		}

		// If the name is confusing, check the implementation below to see the "meta" that's being exported.
		private void writeMetaExportingJsAndDts() {
			FileTools.write(outSrcFile(aId + ".d.ts")).usingWriter(this::writeMetaExportingDts);
			FileTools.write(outSrcFile(aId + ".js")).usingWriter(this::writeMetaExportingJs);
		}

		private void writeMetaExportingDts(Writer writer) throws IOException {
			copyStaticDts(writer);

			writeImports(writer);

			// export meta
			writer.append("export declare namespace meta {\n");
			writer.append("\tconst groupId: string;\n");
			writer.append("\tconst artifactId: string;\n");
			writer.append("\tconst version: string;\n");
			writer.append("}\n");
		}

		private void writeMetaExportingJs(Writer writer) throws IOException {
			writer.append("export const meta = {\n");
			writer.append("\tgroupId: \"" + gId + "\",\n");
			writer.append("\tartifactId: \"" + aId + "\",\n");
			writer.append("\tversion: \"" + version + "\",\n");
			writer.append("}\n");
		}

		private void writeGwtTerminalDts() throws IOException {
			Writer writer = dtsWriter();

			writeImports(writer);

			writeExportInitHcJs(writer);

			copyStaticDts(writer);
		}

		private void writeExportInitHcJs(Writer writer) throws IOException {
			writer.append("export function initHcJs(T: any, hc: any): void;\n\n");
		}

		private void copyStaticDts(Writer writer) throws IOException {
			String staticFileName = staticDtsFileName(aId);
			File staticDtsFile = new File(buildFolder.getParentFile(), "npm/" + staticFileName);
			if (!staticDtsFile.exists())
				return;

			String staticContent = Files.readString(staticDtsFile.toPath());
			writeBlockComment(writer, "Static");
			writer.append(staticContent);
			writer.append("\n");
		}

		private void writeBlockComment(Writer writer, String text) throws IOException {
			writer.append("// ************\n");
			writer.append("// " + text + "\n");
			writer.append("// ************\n\n");
		}

		private static String staticDtsFileName(String artifactId) {
			return artifactId + ".static.d.ts";
		}

		private void writeImports(Writer writer) throws IOException {
			for (VersionedArtifactIdentification dep : deps)
				writer.append("import \"" + TypeScriptWriterHelper.npmPackageFullName(dep).fullName() + "\";\n");
			if (!deps.isEmpty())
				writer.append("\n");
		}

		//
		// package.json
		//

		// TODO, this used to be in the package.json, we need to have this but configurable, maybe read it from the pom.xml
		// "author": "dev.hiconic",
		// "license": "Apache-2.0"
		private void writePackageJson() {
			FileTools.write(outFile("package.json")).usingWriter(this::writePackageJsonTo);
		}

		private void writePackageJsonTo(Writer w) throws IOException {
			String template = loadFromClasspath("package.json");

			String deps = packageDepsAsString();

			String packageJson = template //
					.replace("${GIT_REPO_INFO}", repoInfoForPackageJson()) //
					.replace("${FULL_PACKAGE_NAME}", npmPackageScopedName.fullName()) //
					.replace("${VERSION}", version) //
					.replace("${ARTIFACT_ID}", aId) //
					.replace("${DEPENDENCIES}", deps);

			w.append(packageJson);
		}

		private String packageDepsAsString() {
			if (isCurrentGmCoreApi())
				return "    " + hcJsBaseDep();

			if (deps.isEmpty())
				return "";

			return deps.stream() //
					.map(this::toNpmDependencyString) //
					.collect(Collectors.joining(",\n    ", "    ", ""));
		}

		private String hcJsBaseDep() {
			return "\"@dev.hiconic/hc-js-base\": \"" + extractMajorMinorDotX(version /* gmCoreApi version */) + "\"";
		}

		private String toNpmDependencyString(VersionedArtifactIdentification dep) {
			String majorMinorX = extractMajorMinorDotX(dep.getVersion());

			return "\"" + TypeScriptWriterHelper.npmPackageFullName(dep).fullName() + "\": " + "\"" + majorMinorX + "\"";
		}

		private String extractMajorMinorDotX(String version) {
			String path = StringTools.getSubstringAfterLast(version, ".");

			String majorMinorDot = StringTools.removeLastNCharacters(version, path.length());
			return majorMinorDot + "x";
		}

		private String repoInfoForPackageJson() {
			Maybe<String> resultMaybe = ProcessExecution.runCommand(outputDir, "git", "config", "--get", "remote.origin.url");
			if (!resultMaybe.isSatisfied())
				return "";

			String gitRepoUrl = resultMaybe.get();

			gitRepoUrl = ensureGitRepoUrlIsOfHttpKind(gitRepoUrl);

			StringBuilder sb = new StringBuilder();
			sb.append("\n");
			sb.append("  \"repository\": {\n");
			sb.append("    \"type\": \"git\",\n");
			sb.append("    \"url\": \"git+" + gitRepoUrl + ".git\"\n");
			sb.append("  },");

			return sb.toString();
		}

		//
		// .npmrc
		//

		private void writeNpmrc() {
			if (StringTools.isEmpty(npmRegistryUrl))
				FileTools.write(outFile(".npmrc")).usingWriter(this::writeDefaultNpmrcTo);
			else
				FileTools.write(outFile(".npmrc")).usingWriter(this::writeNpmrcTo);
		}

		private void writeDefaultNpmrcTo(Writer w) throws IOException {
			w.append("//registry.npmjs.org/:_authToken=${NODE_AUTH_TOKEN}");
		}

		private void writeNpmrcTo(Writer w) throws IOException {
			String npmRegistryUrlWithoutProtocol = removeHttpProtocol(npmRegistryUrl);

			String template = loadFromClasspath("npmrc.txt");
			String npmrc = template //
					.replace("${NPM_SCOPE}", npmPackageScopedName.scope()) //
					.replace("${NPM_REGISTRY_URL}", npmRegistryUrl) //
					.replace("${NPM_REGISTRY_URL_WITHOUT_PROTOCOL}", npmRegistryUrlWithoutProtocol) //
			;

			if (npmRegistryUrlWithoutProtocol.contains("npm.pkg.github.com"))
				npmrc = npmrc.replace("NODE_AUTH_TOKEN", "GITHUB_TOKEN");

			w.append(npmrc);
		}

		private String removeHttpProtocol(String url) {
			if (url.startsWith("http://"))
				return url.substring("http://".length());

			if (url.startsWith("https://"))
				return url.substring("https://".length());

			return url;
		}

		private String loadFromClasspath(String templateName) throws IOException {
			URL templateUrl = getClass().getResource(templateName);
			return IOTools.slurp(templateUrl, Charsets.UTF_8.name());
		}

		// ################################################
		// ##. . . . . . . . . Writers. . . . . . . . . .##
		// ################################################

		private Writer dtsWriter() {
			return dtsWriter != null ? dtsWriter : (dtsWriter = writerFor(outSrcFile(aId + ".d.ts")));
		}

		private Writer jsWriter() {
			return jsWriter != null ? jsWriter : (jsWriter = writerFor(outSrcFile(aId + ".js")));
		}

		private Writer writerFor(File file) {
			FileTools.ensureFolderExists(file.getParentFile());

			try {
				return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			} catch (FileNotFoundException e) {
				throw new UncheckedIOException(e);
			}
		}

		private void closeWriters() {
			close(dtsWriter);
			close(jsWriter);
		}

		private void close(Writer writer) {
			if (writer != null)
				try {
					writer.close();
				} catch (IOException e) {
					log.warn("Error while closing writer.", e);
				}
		}

		// ################################################
		// ##. . . . . . . . . Helpers . . . . . . . . . ##
		// ################################################

		private boolean isMarkedAsModel(AnalysisArtifact s) {
			return "model".equals(getPomProperty(s, "archetype"));
		}

		private boolean isJsinterop(AnalysisArtifact artifact) {
			return Boolean.TRUE.toString().equals(getPomProperty(artifact, "jsinterop"));
		}

		private boolean isTypeScriptDeclaration(AnalysisArtifact aa) {
			return NpmPackaging.typeScriptDeclaration.name().equals(getNpmPackagingOf(aa));
		}

		private String getNpmPackagingOf(AnalysisArtifact aa) {
			return getPomProperty(aa, "npmPackaging");
		}

		private String getPomProperty(AnalysisArtifact aa, String p) {
			return aa.getOrigin().getProperties().get(p);
		}

		private boolean isModel(ArtifactIdentification s) {
			return modelArtifactNames.contains(versionlessName(s));
		}

		private boolean isCurrentGmCoreApi() {
			return GM_CORE_API_VERSIONLESS.equals(versionlessName(currentArtifact));
		}

		private String versionlessName(ArtifactIdentification s) {
			return s.getGroupId() + ":" + s.getArtifactId();
		}

		private File outFile(String fileName) {
			return new File(outputDir, fileName);
		}

		private File outSrcFile(String fileName) {
			return new File(outputSrcDir, fileName);
		}

	}

	private enum NpmPackaging {
		typeScriptDeclaration,
		gwtTerminal,
		model
	}

	/* package */ static String ensureGitRepoUrlIsOfHttpKind(String gitRepoUrl) {
		if (!gitRepoUrl.startsWith("git@"))
			return gitRepoUrl;

		// E.G.: git@github.com:hiconic-os/my-repo.git

		// @formatter:off
		String url = gitRepoUrl;
		url = url.replace(":","/" ); 						// replace : with /
		url = "https://" + url.substring("git@".length()); 	// replace git@ with https://
		url = url.substring(0, url.length() - 4); 			// remove .git at the end
		// @formatter:on

		return url;
	}

}
