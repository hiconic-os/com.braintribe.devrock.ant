// ============================================================================
// Braintribe IT-Technologies GmbH - www.braintribe.com
// Copyright Braintribe IT-Technologies GmbH, Austria, 2002-2015 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks.typescript;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Stream;

import com.braintribe.build.ant.tasks.typescript.impl.TfSetupTools;
import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.model.artifact.analysis.AnalysisArtifact;
import com.braintribe.model.generic.GMF;
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.model.generic.reflection.EntityTypes;
import com.braintribe.utils.classloader.ReverseOrderURLClassLoader;

import jsinterop.annotations.JsType;

/**
 * @author peter.gazdik
 */
/* package */ class TsClassLoaderFactory {

	private static final String jsInteropAnnotationPackage = JsType.class.getPackage().getName();
	private static final String gmReflectionPackage = EntityTypes.class.getPackage().getName();

	public static URLClassLoader prepareClassLoader(File buildFolder, List<AnalysisArtifact> solutions) {
		Stream<URL> buildUrl = nullableFileToUrlStream(buildFolder);
		Stream<URL> solutionUrls = extractJarUrlsForSolutions(solutions);

		URL[] urls = Stream.concat(buildUrl, solutionUrls).toArray(URL[]::new);

		return new ReverseOrderURLClassLoader(urls, JsType.class.getClassLoader(), TsClassLoaderFactory::loadFromParentFirst);
	}

	// @formatter:off
	// What are we doing here?
	// Why do we want to load the GM / Reflection classes from our classLoader, and not let them be loaded again from the ReverseOrderClassLoader?
	// There was a problem with MDAs (MetaData annotation which contained a GM Enum), e.g. @RequestMethod(HttpRequestMethod.GET)
	// When loading the annotations of the annotated Request class, this lead to an instance being created, which initializes its T field.
	// I.e. this was called:
	//                  EnumType<HttpRequestMethod> T = EnumTypes.T(HttpRequestMethod.class)
	// Now, if this would be loaded from the ReverseOrderClassLoader, it would lead to a new TypeReflection being created underneath, 
	// which for some reason would fail, because the found implementation would be taken from a different class-loader than the desired interface (GmPlatform).
	// This is unclear to me, but I think it's best not to instantiate a nested TypeReflection, but instead go with the reflection classes of the outer ClassLoader space.
	// In such case, loading the T type detects the Enum (HttpRequestMethod) was loaded by a different ClassLoader and a null is assigned to the T field. 
	// @formatter:on

	private static boolean loadFromParentFirst(String className) {
		return className.startsWith(jsInteropAnnotationPackage) || //
				className.startsWith(gmReflectionPackage) || //
				className.equals(Initializer.class.getName()) || //
				className.equals(GMF.class.getName());
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

}
