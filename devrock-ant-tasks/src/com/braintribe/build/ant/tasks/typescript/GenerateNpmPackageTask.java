// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript;

import static com.braintribe.model.typescript.TypeScriptWriterHelper.createCustomGmTypeFilter;
import static com.braintribe.model.typescript.TypeScriptWriterHelper.jsinteropDtsFileName;
import static com.braintribe.model.typescript.TypeScriptWriterHelper.staticDtsFileName;
import static com.braintribe.model.typescript.TypeScriptWriterHelper.typesDtsFileName;
import static com.braintribe.model.typescript.TypeScriptWriterHelper.writeTripleSlashReference;
import static com.braintribe.model.typescript.TypeScriptWriterHelper.writeTripleSlashReferenceToMain;
import static com.braintribe.utils.SysPrint.spOut;
import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.concat;
import static com.braintribe.utils.lcd.CollectionTools2.newSet;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.partitioningBy;

import java.io.File;
import java.io.IOException;
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
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.UnknownEnumException;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
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
import com.braintribe.model.version.Version;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.IOTools;
import com.braintribe.utils.StringTools;
import com.braintribe.utils.lcd.CollectionTools2;
import com.braintribe.utils.lcd.NullSafe;

/**
 * @author peter.gazdik
 */
public class GenerateNpmPackageTask extends Task {

	private static final String CLASS_FILE_SUFFIX = ".class";
	private static final int CLASS_FILE_SUFFIX_LENGTH = CLASS_FILE_SUFFIX.length();

	private static final String GITHUB_ORG = "hiconic-os";

	private String npmPackagingStr;
	private File buildFolder;
	private File outputDir;
	private String resolutionId;
	private String npmPackageName;
	private boolean generateNpmrc;

	// @formatter:off
	// doesn't exist for GWT terminals, but we use it to get the parent and navigate to possible static .d.ts files
	@Required public void setNpmPackaging(String npmPackaging) { this.npmPackagingStr = npmPackaging; }
	@Required public void setBuildFolder(File buildFolder) { this.buildFolder = buildFolder; }
	public void setResolutionId(String resolutionId) { this.resolutionId = resolutionId; }

	/** Optional, defaults to artifactId.
	 * For GWT terminals, the value for pretty build is "${artifactId}-dev. */
	public void setNpmPackageName(String npmPackageName) { this.npmPackageName = npmPackageName;}
	/** Optional, only for local testing and the npmrc is for GitHub (i.e. incompatible with npmjs-only package.json we generate) */
	public void setGenerateNpmrc(boolean generateNpmrc) { this.generateNpmrc = generateNpmrc; }
	// @formatter:on

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@Override
	public void execute() throws BuildException {
		DrAntTools.runAndPrintStacktraceIfNonBuildException(this::_execute);
	}

	private void _execute() throws BuildException {
		new GenerateNpmPackageExecution().run();
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
		private final AnalysisArtifact currentArtifact = resolveCurrentProject();

		private final String gId = currentArtifact.getGroupId();
		private final String aId = currentArtifact.getArtifactId();
		private final String version = currentArtifact.getVersion();

		private final File outputSrcDir = new File(outputDir, "src");

		private List<AnalysisArtifact> deps = emptyList();

		private NpmPackaging npmPackaging;
		private List<Class<?>> regularClasses = emptyList();
		private List<Class<?>> gmClasses;
		private List<Class<?>> gmClassesForwarded;
		private List<GmType> gmTypesDeclared;
		private List<GmType> gmTypesForwarded;
		private List<GmType> gmTypesAll;

		private String problemMsg;

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

		public void run() {
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

		private void writeNpmPackage() {
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

			writePackageJson();
			writeNpmrcIfNeeded();
		}

		private void writeGwtTerminalPackage() {
			resolveDepsAllModels();

			addClassesFromDepsToRegularClasses();

			writeJsinteropDts();

			writeMainGwtTerminalDts();
		}

		private void writeModelPackage() {
			resolveDepsDirectModelsOnly();

			analyzeLocalClasses();

			writeTypesDts();

			writeMainModelEnsuringJsAndDTs();
		}

		private void writeTypeScriptDeclarationPackage() {
			if (isCurrentGmCoreApi()) {
				analyzeLocalClasses();
				writeTypesDts();
				// no resolveDeps needed --> gm-core-api doesn't depend on any model or dts artifact

			} else {
				resolveDepsAllModels();
			}

			addClassesFromDepsToRegularClasses();

			writeJsinteropDts();

			// we only expect model types in gm-core-api, and we don't ensure them
			if (isCurrentGmCoreApi())
				writeMainModelEnsuringJsAndDTs();
			else
				writeMainMetaExportingJsAndDts();
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

		private void writeTypesDts() {
			FileTools.write(outSrcFile(typesDtsFileName(aId))).usingWriter(this::writeTypesDts);
		}

		private void writeTypesDts(Writer writer) throws IOException {
			writeTripleSlashReferenceToMain(aId, writer);
			TypeScriptWriterForModels.write(gmTypesDeclared, jsNameResolver, writer);
		}

		private void writeJsinteropDts() {
			FileTools.write(outSrcFile(jsinteropDtsFileName(aId))).usingWriter(this::writeJsinteropDts);
		}

		private void writeJsinteropDts(Writer writer) throws IOException {
			writeTripleSlashReferenceToMain(aId, writer);
			TypeScriptWriterForClasses.write(regularClasses, customGmTypeFilter, writer);
		}

		private void writeMainModelEnsuringJsAndDTs() {
			ModelEnsuringContext meContext = ModelEnsuringContext.createForNpm(gmTypesAll, gId, aId, version, deps);

			FileTools.write(outSrcFile(aId + ".d.ts")).usingWriter(writer -> writeMainModelEnsuringJsAndDTs(meContext, writer));
			FileTools.write(outSrcFile(aId + ".js")).usingWriter(writer -> ModelEnsuringJsWriter.writeJs(meContext, writer));
		}

		private void writeMainModelEnsuringJsAndDTs(ModelEnsuringContext meContext, Writer writer) throws IOException {
			if (isCurrentGmCoreApi()) {
				if (copyStaticDts())
					writeTripleSlashReference(staticDtsFileName(aId), writer);
				writeTripleSlashReference(jsinteropDtsFileName(aId), writer);
			}

			ModelEnsuringDTsWriter.writeDts(meContext, writer);
		}

		// If the name is confusing, check the implementation below to see the "meta" that's being exported.
		private void writeMainMetaExportingJsAndDts() {
			FileTools.write(outSrcFile(aId + ".d.ts")).usingWriter(this::writeMetaExportingDts);
			FileTools.write(outSrcFile(aId + ".js")).usingWriter(this::writeMetaExportingJs);
		}

		private void writeMetaExportingDts(Writer writer) throws IOException {
			// triple slashes
			if (copyStaticDts())
				writeTripleSlashReference(staticDtsFileName(aId), writer);
			writeTripleSlashReference(jsinteropDtsFileName(aId), writer);
			writer.append("\n");

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

		private void writeMainGwtTerminalDts() {
			FileTools.write(outSrcFile(aId + ".d.ts")).usingWriter(this::writeMainGwtTerminalDts);

		}
		private void writeMainGwtTerminalDts(Writer writer) throws IOException {
			if (copyStaticDts())
				writeTripleSlashReference(staticDtsFileName(aId), writer);
			writeTripleSlashReference(jsinteropDtsFileName(aId), writer);
			writer.append("\n");

			writeImports(writer);
		}

		private boolean copyStaticDts() {
			String staticFileName = staticDtsFileName(aId);
			File staticDtsFile = new File(buildFolder.getParentFile(), "npm/" + staticFileName);
			if (!staticDtsFile.exists())
				return false;

			FileTools.write(outSrcFile(staticFileName)).usingWriter(writer -> {
				writeTripleSlashReferenceToMain(aId, writer);
				String staticContent = Files.readString(staticDtsFile.toPath());
				writer.append(staticContent);
			});

			return true;
		}

		private void writeImports(Writer writer) throws IOException {
			for (VersionedArtifactIdentification dep : deps)
				writer.append("import \"" + TypeScriptWriterHelper.npmPackageFullName(dep) + "\";\n");
			if (!deps.isEmpty())
				writer.append("\n");
		}

		private void writePackageJson() {
			FileTools.write(outFile("package.json")).usingWriter(this::writePackageJsonTo);
		}

		private void writePackageJsonTo(Writer w) throws IOException {
			String template = loadFromClasspath("package.json");

			String groupId = currentArtifact.getGroupId();
			String packageSuffix = NullSafe.get(npmPackageName, currentArtifact.getArtifactId());

			String fullPackageName = TypeScriptWriterHelper.npmPackageFullName(groupId, packageSuffix);

			String deps = packageDepsAsString();

			String packageJson = template //
					.replace("${GITHUB_ORG}", GITHUB_ORG) //
					.replace("${FULL_PACKAGE_NAME}", fullPackageName) //
					.replace("${VERSION}", currentArtifact.getVersion()) //
					.replace("${ARTIFACT_ID}", currentArtifact.getArtifactId()) //
					.replace("${GROUP_ID}", groupId) //
					.replace("${DEPENDENCIES}", deps);

			w.append(packageJson);
		}

		private String packageDepsAsString() {
			if (deps.isEmpty())
				return "";
			else
				return deps.stream() //
						.map(this::toNpmDependencyString) //
						.collect(Collectors.joining(",\n    ", "    ", ""));
		}

		private String toNpmDependencyString(VersionedArtifactIdentification dep) {
			String version = dep.getVersion();
			String path = StringTools.getSubstringAfterLast(version, ".");

			String majorMinorDot = StringTools.removeLastNCharacters(version, path.length());
			String majorMinorX = majorMinorDot + "x";

			return "\"" + TypeScriptWriterHelper.npmPackageFullName(dep) + "\": " + "\"" + majorMinorX + "\"";
		}

		private void writeNpmrcIfNeeded() {
			if (generateNpmrc)
				FileTools.write(outFile(".npmrc")).usingWriter(this::writeNpmrcTo);
		}

		private void writeNpmrcTo(Writer w) throws IOException {
			String template = loadFromClasspath("npmrc");
			String npmrc = template.replace("${GITHUB_ORG}", GITHUB_ORG);

			w.append(npmrc);
		}

		private String loadFromClasspath(String templateName) throws IOException {
			URL templateUrl = getClass().getResource(templateName);
			return IOTools.slurp(templateUrl, Charsets.UTF_8.name());
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

	public static class Tmp {

		public static void main(String[] args) {
			String version = "2.1.56";
			String suffix = StringTools.getSubstringAfterLast(version, ".");

			String majorMinorDot = StringTools.removeLastNCharacters(version, suffix.length());
			String majorMinorX = majorMinorDot + "x";

			spOut(majorMinorX);

		}
	}
}
