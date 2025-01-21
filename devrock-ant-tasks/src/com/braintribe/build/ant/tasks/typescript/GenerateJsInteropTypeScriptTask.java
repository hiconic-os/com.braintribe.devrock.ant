// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript;

import static com.braintribe.model.typescript.TypeScriptWriterHelper.createCustomGmTypeFilter;
import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static com.braintribe.utils.lcd.CollectionTools2.newList;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.partitioningBy;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.utils.DrAntTools;
import com.braintribe.cfg.Configurable;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.artifact.analysis.AnalysisArtifactResolution;
import com.braintribe.model.artifact.analysis.AnalysisDependency;
import com.braintribe.model.artifact.essential.ArtifactIdentification;
import com.braintribe.model.artifact.essential.VersionedArtifactIdentification;
import com.braintribe.model.generic.mdec.ModelDeclaration;
import com.braintribe.model.jvm.reflection.ModelDeclarationParser;
import com.braintribe.model.meta.GmType;
import com.braintribe.model.processing.itw.analysis.JavaTypeAnalysis;
import com.braintribe.model.typescript.ModelEnsuringContext;
import com.braintribe.model.typescript.ModelEnsuringDTsWriter;
import com.braintribe.model.typescript.ModelEnsuringJsWriter;
import com.braintribe.model.typescript.TypeScriptWriterForClasses;
import com.braintribe.model.typescript.TypeScriptWriterForModels;
import com.braintribe.model.typescript.TypeScriptWriterHelper;
import com.braintribe.model.version.Version;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.StringTools;

/**
 * @author peter.gazdik
 */
public class GenerateJsInteropTypeScriptTask extends Task {

	private static final String CLASS_FILE_SUFFIX = ".class";
	private static final int CLASS_FILE_SUFFIX_LENGTH = CLASS_FILE_SUFFIX.length();

	private File buildFolder;
	private File solutionListFile;
	private File outputDir;
	private String resolutionRefId;

	public void setBuildFolder(File buildFolder) {
		this.buildFolder = buildFolder;
	}

	public void setSolutionListFile(File solutionListFile) {
		this.solutionListFile = solutionListFile;
	}

	public void setOutputDir(File outputDir) {
		this.outputDir = outputDir;
	}

	@Configurable
	public void setResolutionRefId(String resolutionRefId) {
		this.resolutionRefId = resolutionRefId;
	}

	@Override
	public void execute() throws BuildException {
		DrAntTools.runAndPrintStacktraceIfNonBuildException(this::_execute);
	}

	private void _execute() throws BuildException {
		new GenerateJsInteropTsExecution().run();
	}

	private class GenerateJsInteropTsExecution {

		private static final String GM_CORE_API_VERSIONLESS = "com.braintribe.gm:gm-core-api";

		private final AnalysisArtifactResolution resolution = getProject()
				.getReference(Optional.ofNullable(resolutionRefId).orElse("LAST-DEPENDENCIES-RESOLUTION"));
		private final List<AnalysisArtifact> dependencySolutions = resolution.getSolutions();
		private final ClassLoader classLoader = TsClassLoaderFactory.prepareClassLoader(buildFolder, dependencySolutions);
		private final Function<Class<?>, String> jsNameResolver = TypeScriptWriterHelper.jsNameResolver(classLoader);
		private final Predicate<Class<?>> customGmTypeFilter = createCustomGmTypeFilter(classLoader);
		private final Path buildFolderPath = buildFolder.toPath().toAbsolutePath();
		private final Set<String> modelArtifactNames = resolveModelArtifactNames();
		private final AnalysisArtifact currentArtifact = resolveCurrentProject();

		private final String gId = currentArtifact.getGroupId();
		private final String aId = currentArtifact.getArtifactId();
		private final String version = currentArtifact.getVersion();

		private List<Class<?>> regularClasses;
		private List<Class<?>> gmClasses;
		private List<Class<?>> gmClassesForwarded;
		private List<GmType> gmTypesDeclared;
		private List<GmType> gmTypesForwarded;
		private List<GmType> gmTypesAll;

		private String problemMsg;

		private Set<String> resolveModelArtifactNames() {
			Set<String> result = asSet("com.braintribe.gm:gm-core-api");

			for (AnalysisArtifact s : dependencySolutions)
				if (isMarkedAsModel(s))
					result.add(versionlessName(s));

			return result;
		}

		private AnalysisArtifact resolveCurrentProject() {
			return (AnalysisArtifact) resolution.getTerminals().get(0);
		}

		public void run() {
			validate();
			analyzeClasses();

			writeClassAndModelsDTs();

			if (isCurrentArtifactModel())
				writeModelEnsuringJsAndDTsIf();
		}

		private void validate() {
			if (findProblem())
				throw new BuildException(problemMsg);
		}

		private boolean findProblem() {
			return isProblem(buildFolder == null, "buildFolder not specified")
					|| isProblem(!buildFolder.isDirectory(), "buildFolder is not an existing directory: " + buildFolder.getAbsolutePath())
					|| isProblem(solutionListFile == null, "solutionListFile not specified")
					|| isProblem(!solutionListFile.exists(), "solutionListFile does not exist: " + solutionListFile.getAbsolutePath());
		}

		private boolean isProblem(boolean test, String msg) {
			problemMsg = msg;
			return test;
		}

		private void analyzeClasses() {
			JavaTypeAnalysis jta = new JavaTypeAnalysis();
			jta.setClassLoader(classLoader);
			jta.setRequireEnumBase(rootModelMajor() >= 2);

			Map<Boolean, List<Class<?>>> classes = gmAndRegularClasses();

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
			return dependencySolutions.stream() //
					.filter(ds -> "com.braintribe.gm".equals(ds.getGroupId()) && "root-model".equals(ds.getArtifactId())) //
					.mapToInt(ds -> Version.parse(ds.getVersion()).getMajor()) //
					.findFirst() //
					.orElse(1);
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
			List<GmType> result = newList(gmTypesDeclared);
			result.addAll(gmTypesForwarded);
			return result;
		}

		private boolean isCurrentArtifactModel() {
			return isMarkedAsModel(currentArtifact) || GM_CORE_API_VERSIONLESS.equals(versionlessName(currentArtifact));
		}

		private void writeClassAndModelsDTs() {
			FileTools.ensureFolderExists(outputDir);

			FileTools.write(outFile(TypeScriptWriterHelper.dtsFileName(aId))).usingWriter(this::writeClassAndModelsDTs);
		}

		private void writeClassAndModelsDTs(Writer writer) throws IOException {
			TypeScriptWriterHelper.writeTripleSlashReferences(getDependencies(false), writer);
			TypeScriptWriterForModels.write(gmTypesDeclared, jsNameResolver, writer);
			TypeScriptWriterForClasses.write(regularClasses, customGmTypeFilter, writer);
		}

		// Result is partitioned based on given class being a GM type (enum or entity) or a regular java class
		private Map<Boolean, List<Class<?>>> gmAndRegularClasses() {
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

		private Class<?> toClassIfPossible(String className) {
			Class<?> result = getClassOrNull(className);
			if (result == null)
				log("Class not found and will be ignored from TypeScript generation: " + className);

			return result;
		}

		private Class<?> getClassOrNull(String className) {
			try {
				return Class.forName(className, false, classLoader);
			} catch (ClassNotFoundException | LinkageError e) {
				return null;
			}
		}

		private void writeModelEnsuringJsAndDTsIf() {
			ModelEnsuringContext meContext = ModelEnsuringContext.create(gmTypesAll, gId, aId, version, getDependencies(true));

			FileTools.write(outFile(meContext.dtsFileName())).usingWriter(writer -> ModelEnsuringDTsWriter.writeDts(meContext, writer));
			FileTools.write(outFile(meContext.jsFileName())).usingWriter(writer -> ModelEnsuringJsWriter.writeJs(meContext, writer));
		}

		private List<VersionedArtifactIdentification> getDependencies(boolean onlyModels) {
			Predicate<AnalysisDependency> filter = onlyModels ? this::isModel : this::isJsInteroppedOrModel;

			return currentArtifact.getDependencies().stream() //
					.filter(filter) //
					.map(this::dependencyToArtifactIdentification) //
					.collect(Collectors.toList());
		}

		private VersionedArtifactIdentification dependencyToArtifactIdentification(AnalysisDependency dep) {
			return VersionedArtifactIdentification.create(dep.getGroupId(), dep.getArtifactId(), getShortNotationVersion(dep));
		}

		private String getShortNotationVersion(AnalysisDependency d) {
			return d.getOrigin().getVersion().asShortNotation();
		}

		private boolean isJsInteroppedOrModel(AnalysisDependency d) {
			return d.getOrigin().getTags().contains("js") || modelArtifactNames.contains(versionlessName(d));
		}

		private boolean isMarkedAsModel(AnalysisArtifact s) {
			return "model".equals(s.getOrigin().getProperties().get("archetype"));
		}

		private boolean isModel(ArtifactIdentification s) {
			return modelArtifactNames.contains(versionlessName(s));
		}

		private String versionlessName(ArtifactIdentification s) {
			return s.getGroupId() + ":" + s.getArtifactId();
		}

		private File outFile(String fileName) {
			return new File(outputDir, fileName);
		}

	}

}
