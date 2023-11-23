// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArtifactBundle {
	
	private String artifactName;
	private static final Pattern i18nInterfacePattern = Pattern.compile("public\\s+interface\\s+(\\w+)\\s+extends\\s+(Messages|Constants|ConstantsWithLookup|Localizable)\\s+\\{", Pattern.MULTILINE);
	private static final Pattern i18nMethodPattern = Pattern.compile("String\\s+(\\w+)\\(.*\\)\\s*;", Pattern.MULTILINE);
	private static final Pattern i18nPackagePattern = Pattern.compile("package\\s*(.*)\\s*;", Pattern.MULTILINE);
	
	private Map<String, TranslationBundle> translationBundles = new LinkedHashMap<String, TranslationBundle>();
	
	public String getArtifactName() {
		return artifactName;
	}
	
	public void setArtifactName(String artifactName) {
		this.artifactName = artifactName;
	}

	public Map<String, TranslationBundle> getTranslationBundles() {
		return translationBundles;
	}
	
	public TranslationBundle getTranslationBundle(String className) {
		return translationBundles.get(className);
	}

	public TranslationBundle aquireTranslationBundle(String className) {
		TranslationBundle translationBundle = translationBundles.get(className);
		if (translationBundle == null) {
			translationBundle = new TranslationBundle();
			translationBundle.setClassName(className);
			translationBundles.put(className, translationBundle);
		}
		return translationBundle;
	}
	
	public void addTranslationBundlesFromSource(File sourceFolder) {
		for (String fileName : sourceFolder.list()) {
			fileName = sourceFolder.getAbsolutePath() + File.separator + fileName; 
			File file = new File(fileName);
			if (file.isDirectory()) {
				addTranslationBundlesFromSource(file);
			} else {
				addTranslationBundle(file);
			}
		}
	}
	
	/*
	
	if (artifactBundle != null) {
		if (artifactBundle.getTranslationBundleList() != null) {
			if (returnArtifactBundle.getTranslationBundleList() == null) {
				List<TranslationBundle> list = new LinkedList<TranslationBundle>();
				returnArtifactBundle.setTranslationBundleList(list);
			}
			returnArtifactBundle.getTranslationBundleList().addAll(artifactBundle.getTranslationBundleList());
		}
	}


	 */
	
	private void addTranslationBundle(File sourceFile) {
		String fileName = sourceFile.getName();
		if (fileName.endsWith(".java")) {
			SourceDescription sourceDescription = getSourceDescriptionIfTranslationClass(sourceFile);
			if (sourceDescription != null) {
				TranslationBundle translationBundle = aquireTranslationBundle(sourceDescription.getClassName());
				translationBundle.getExistingProperties().addAll(sourceDescription.getExistingProperties());
				translationBundle.addTranslationsFromJavaFile(sourceFile);
			}
		} else if (fileName.endsWith("custom.texts.properties")) { //Getting Spring i18n file
			SourceDescription sourceDescription = getSourceDescriptionFromTranslationProperty(sourceFile);
			TranslationBundle translationBundle = aquireTranslationBundle(sourceDescription.getClassName());
			translationBundle.getExistingProperties().addAll(sourceDescription.getExistingProperties());
			translationBundle.addTranslationsFromPropertyFile(sourceFile);
		}
	}
	
	/**
	 * This method returns null if the sourceFile is not a Translation interface. It returns the package name of the interface, otherwise.
	 * @param sourceFile
	 * @return
	 */
	private SourceDescription getSourceDescriptionIfTranslationClass(File sourceFile) {
		SourceDescription sourceDescription = null;
		FileReader reader = null;
		StringWriter writer = null;
		try {
			writer = new StringWriter();
			reader = new FileReader(sourceFile);
			
			char buffer[] = new char[4096]; 
			int num;
			while ((num = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, num);
			}
			
			String source = writer.getBuffer().toString();
			
			// do we have a interface for internationalization here?
			Matcher interfaceMatcher = i18nInterfacePattern.matcher(source);
			if (interfaceMatcher.find()) {
				// yes we have so lets extract the class name first;
				String className = interfaceMatcher.group(1);
				
				// now lets find the package name;
				Matcher packageMatcher = i18nPackagePattern.matcher(source);

				if (packageMatcher.find()) {
					// only continue when package is present
					String packageName = packageMatcher.group(1);
				
					sourceDescription = new SourceDescription();
					sourceDescription.setClassName(packageName + "." + className);

					Matcher methodMatcher = i18nMethodPattern.matcher(source);
					
					while (methodMatcher.find()) {
						String propertyName = methodMatcher.group(1);
						sourceDescription.addExistingProperty(propertyName);
					}
				}
			}
		} catch (FileNotFoundException ignore) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
				if (writer != null) 
					writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return sourceDescription;
	}
	
	private SourceDescription getSourceDescriptionFromTranslationProperty(File sourceFile) {
		SourceDescription sourceDescription = new SourceDescription();
		BufferedReader input = null;
		try {
			String filePath = sourceFile.getPath();
			sourceDescription.setClassName(filePath);
			input = new BufferedReader(new FileReader(sourceFile));
			String line;
			
			while ((line = input.readLine()) != null) {
				if (line.startsWith("#")) {
					break;
				}
				
				int index = line.indexOf("=");
				if (index != -1) {
					sourceDescription.addExistingProperty(line.substring(0, index));
				}
			}
			
		} catch (FileNotFoundException ignore) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return sourceDescription;
	}
	
	private static class SourceDescription {
		private String className;
		private Set<String> existingProperties = new HashSet<String>();
		
		public void setClassName(String className) {
			this.className = className;
		}
		
		public String getClassName() {
			return className;
		}
		
		public Set<String> getExistingProperties() {
			return existingProperties;
		}
		
		public void addExistingProperty(String existingProperty) {
			existingProperties.add(existingProperty);
		}
		
	}

	public void syncFrom(ArtifactBundle artifactBundle, SynchronizationParams params) {
		for (TranslationBundle translationBundle: translationBundles.values()) {
			TranslationBundle otherTranslationBundle = artifactBundle.getTranslationBundle(
					translationBundle.getClassName());
			
			if (otherTranslationBundle != null) {
				translationBundle.syncFrom(otherTranslationBundle, params);
			}
		}
		
	}
	
	public static void main(String[] args) {
		String test = "package com.x.y;\npublic interface LocalizedText extends Messages {";
		Matcher m = i18nInterfacePattern.matcher(test);
		if (m.find()) {
			System.out.println(m.group(1));
		}
	}
}
