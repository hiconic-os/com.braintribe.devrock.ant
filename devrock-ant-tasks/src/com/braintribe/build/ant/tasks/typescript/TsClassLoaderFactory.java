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
import com.braintribe.model.generic.annotation.Initializer;
import com.braintribe.utils.classloader.ReverseOrderURLClassLoader;

import jsinterop.annotations.JsType;

/**
 * @author peter.gazdik
 */
/* package */ class TsClassLoaderFactory {

	private static final String jsInteropAnnotationPackage = JsType.class.getPackage().getName();

	public static URLClassLoader prepareClassLoader(File buildFolder, List<AnalysisArtifact> solutions) {
		URL buildUrl = fileToUrl(buildFolder);
		Stream<URL> solutionUrls = extractJarUrlsForSolutions(solutions);

		URL[] urls = Stream.concat(Stream.of(buildUrl), solutionUrls).toArray(URL[]::new);

		return new ReverseOrderURLClassLoader(urls, JsType.class.getClassLoader(), TsClassLoaderFactory::loadFromParentFirst);
	}

	private static boolean loadFromParentFirst(String className) {
		return className.startsWith(jsInteropAnnotationPackage) || //
				className.equals(Initializer.class.getName());
	}

	private static Stream<URL> extractJarUrlsForSolutions(List<AnalysisArtifact> solutions) {
		return solutions.stream() //
				.map(TsClassLoaderFactory::getJar) //
				.map(TsClassLoaderFactory::fileToUrl);
	}

	private static File getJar(AnalysisArtifact artifact) {
		return TfSetupTools.getPartFile(artifact, PartIdentifications.jar);
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
