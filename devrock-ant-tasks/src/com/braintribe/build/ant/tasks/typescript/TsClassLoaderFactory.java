// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript;

import static com.braintribe.utils.lcd.CollectionTools2.asSet;
import static java.util.Collections.emptySet;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.braintribe.build.ant.tasks.typescript.impl.TfSetupTools;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.model.generic.reflection.EnumType;
import com.braintribe.model.generic.reflection.EnumTypes;
import com.braintribe.model.generic.reflection.GM_INITIALIZATION;
import com.braintribe.model.generic.reflection.GenericModelTypeReflection;
import com.braintribe.utils.classloader.ReverseOrderURLClassLoader;

import jsinterop.annotations.JsType;

/**
 * @author peter.gazdik
 */
/* package */ class TsClassLoaderFactory {

	private static final String jsInteropAnnotationPackage = JsType.class.getPackage().getName();
	private static final Set<String> gmReflectionClassNames = asSet( //
			EntityTypes.class.getName(), EntityType.class.getName(), //
			EnumTypes.class.getName(), EnumType.class.getName(), //
			GenericModelTypeReflection.class.getName(), //
			GMF.class.getName() //
	);

	private static Set<String> parentFirstReflectionClassNames = emptySet();

	public static URLClassLoader prepareClassLoader(File buildFolder, List<AnalysisArtifact> solutions) {
		Stream<URL> buildUrl = nullableFileToUrlStream(buildFolder);
		Stream<URL> solutionUrls = extractJarUrlsForSolutions(solutions);

		URL[] urls = Stream.concat(buildUrl, solutionUrls).toArray(URL[]::new);

		ReverseOrderURLClassLoader classLoader = new ReverseOrderURLClassLoader(urls, JsType.class.getClassLoader(),
				TsClassLoaderFactory::loadFromParentFirst);

		if (!disableGmTypesInitialization(classLoader))
			parentFirstReflectionClassNames = gmReflectionClassNames;

		return classLoader;
	}

	// @formatter:off
	// What are we doing here?
	// Why do we want to load the EntityTypes/EnumTypes from our (outer) classLoader, and not let them be loaded again from the ReverseOrderClassLoader?
	// There was a problem with MDAs (MetaData annotation which contained a GM Enum), e.g. @RequestMethod(HttpRequestMethod.GET)
	// When loading the annotations of the annotated Request class, this lead to an instance being created, which initializes its T field.
	// I.e. this was called:
	//                  EnumType<HttpRequestMethod> T = EnumTypes.T(HttpRequestMethod.class)
	// Now, if EnumTypes was be loaded by the ReverseOrderClassLoader, it would lead to a new TypeReflection being created underneath ,
	// and also a new GmPlatform initialization. That would fail as the found implementation would for some reason be loaded by the outer ClassLoader,
	// but the desired interface (GmPlatform) by the ReverseOrderClassLoader, this there would be a mismatch (no idea why).
	// Either way, I think it's better not to instantiate a nested GmPlatform + TypeReflection, but go with the reflection classes of the outer ClassLoader space.
	// In such case, loading the T type detects the Enum (HttpRequestMethod) was loaded by a different ClassLoader and a null is assigned to the T field. 
	// @formatter:on

	private static boolean loadFromParentFirst(String className) {
		return className.startsWith(jsInteropAnnotationPackage) || //
				className.equals(Initializer.class.getName()) || //
				parentFirstReflectionClassNames.contains(className) //
		;
	}

	private static Stream<URL> extractJarUrlsForSolutions(List<AnalysisArtifact> solutions) {
		return solutions.stream() //
				.map(TsClassLoaderFactory::getJar) //
				.map(TsClassLoaderFactory::fileToUrl);
	}

	private static File getJar(AnalysisArtifact artifact) {
		return TfSetupTools.getPartFile(artifact, PartIdentifications.jar);
	}

	private static Stream<URL> nullableFileToUrlStream(File file) {
		return file == null || !file.exists() ? Stream.empty() : Stream.of(fileToUrl(file));
	}

	private static URL fileToUrl(File file) {
		URI uri = file.toURI();

		try {
			return uri.toURL();

		} catch (MalformedURLException e) {
			throw new RuntimeException("Cannot convert URI to URL: " + uri, e);
		}
	}

	/**
	 * Sets {@link GM_INITIALIZATION#T_LITERAL_INIT_ENABLED} to false;
	 */
	private static boolean disableGmTypesInitialization(ReverseOrderURLClassLoader classLoader) {
		String className = GM_INITIALIZATION.class.getName();
		String fieldName = "T_LITERAL_INIT_ENABLED";

		System.out.println("Deactivationg T Literal initialization for scanned classes.");
		Class<?> clazz = loadClassOrNull(classLoader, className);
		if (clazz == null) {
			System.out.println(className + " not found at all (UNEXPECTED). Will continue with legacy behavior.");
			return false;
		}

		if (clazz.getClassLoader() != classLoader) {
			System.out.println(clazz.getSimpleName() + " not found in this artifact's deps. Will continue with legacy behavior.");
			return false;
		}

		try {
			Field declaredField = clazz.getDeclaredField(fieldName);
			declaredField.set(null, Boolean.FALSE);

			System.out.println("T literal initialization successfully disabled.");
			return true;

		} catch (Exception e) {
			System.out.println("Exception while trying to disabled T literal initialization of GM types. Will continue with legacy behavior");
			e.printStackTrace();
			return false;
		}
	}

	private static Class<?> loadClassOrNull(ReverseOrderURLClassLoader classLoader, String className) {
		try {
			return Class.forName(className, true, classLoader);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

}
