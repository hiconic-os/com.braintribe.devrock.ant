// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;

import com.braintribe.build.ant.translation.model.ArtifactBundle;
import com.braintribe.build.ant.translation.model.Model;
import com.braintribe.build.ant.translation.model.Translation;
import com.braintribe.build.ant.translation.model.TranslationBundle;

/**
 * This class contains Util methods for the translation parsing.
 * It has functionalities for both exporting the current .properties to a .csv file format, and importing from a .csv to the source
 * @author michel.docouto
 *
 */
public class Util {
	
	private static File sourceFolder;
		
	
	/**
	 * This method verifies if the source path exists and is a directory
	 * @param sourcePath
	 */
	private static void verifySourcePath(String sourcePath) {
		File sourceFolder = new File(sourcePath);
		if (!sourceFolder.exists()) {
			System.out.println("The given sourcePath (" + sourcePath + ") does not exist.");
			System.exit(-1);
		}
		if (!sourceFolder.isDirectory()) {
			System.out.println("The given sourcePath (" + sourcePath + ") is not a directory.");
			System.exit(-2);
		}
		
		Util.sourceFolder = sourceFolder;
	}
	

	
	/**
	 * This method returns the Translation list
	 * @param filePath
	 * @return
	 */
	/*private static List<Translation> getTranslationList(String filePath) {
		List<Translation> translationList = new LinkedList<Translation>();
		String folderPath = filePath.substring(0, filePath.lastIndexOf(File.separatorChar) + 1);
		String fileName = filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1, filePath.indexOf(".java"));
		File folderFile = new File(folderPath);
		List<PropertyContents> propertyContentsList = new LinkedList<PropertyContents>();
		for (String propertyFilePath : folderFile.list()) {
			propertyFilePath = folderPath + propertyFilePath;
			if (propertyFilePath.contains(fileName) && propertyFilePath.endsWith(".properties")) {
				propertyContentsList.add(getPropertyContents(propertyFilePath));
			}
		}
		
		//Getting the keySet
		PropertyContents firstPropertyContents = propertyContentsList.get(0);
		Set<String> keySet = firstPropertyContents.getTranslationsMap().keySet();
		
		for (String key : keySet) {
			if (existingProperties.contains(key)) { //key exists in the source?
				Translation translation = new Translation();
				Map<String, String> translations = new LinkedHashMap<String, String>();
				translation.setKey(key);
				for (PropertyContents propertyContents : propertyContentsList) {
					String languageName = propertyContents.getLanguageName();
					String value = propertyContents.getTranslationsMap().get(key);
					translations.put(languageName, value);
				}
				translation.setTranslations(translations);
				translationList.add(translation);
			}
		}
		
		return translationList;
	}*/
	
	/**
	 * This method returns the properties contents for a given property root
	 * @param propertyFilePath
	 * @return
	 */
	/*private static PropertyContents getPropertyContents(String propertyFilePath) {
		PropertyContents propertyContents = new PropertyContents();
		String propertyFileName = propertyFilePath.substring(propertyFilePath.lastIndexOf(File.separatorChar) + 1);
		String propertyLanguage = "";
		if (propertyFileName.contains("_")) {
			propertyLanguage = propertyFileName.substring(propertyFileName.lastIndexOf("_") + 1, propertyFileName.indexOf(".properties"));
		}
		propertyContents.setLanguageName(propertyLanguage);
		
		Map<String, String> translationsMap = getOriginalPropertyTranslationsMap(propertyFilePath);
		propertyContents.setTranslationsMap(translationsMap);
		
		return propertyContents;
	}*/
	
	
	
	/**
	 * This method writes the model to the source path, creating/updating the .properties with the translations
	 * @param model
	 * @param sourcePath
	 */
	public static void writeModelToSourcePath(Model model, String sourcePath) {
		if (!sourcePath.endsWith(File.separator)) {
			sourcePath = sourcePath.concat(File.separator);
		}
		verifySourcePath(sourcePath);
		
		if (model != null) {
			for (ArtifactBundle artifactBundle : model.getArtifactBundles().values()) {
				writeArtifactBundle(artifactBundle);
			}
		}
	}
	
	/**
	 * This method writes an ArtifactBundle to the source
	 * @param artifactBundle
	 */
	private static void writeArtifactBundle(ArtifactBundle artifactBundle) {
		if (artifactBundle != null) {
			for (TranslationBundle translationBundle : artifactBundle.getTranslationBundles().values()) {
				writeTranslationBundle(translationBundle);
			}
		}
	}
	
	/**
	 * This method writes a TranslationBundle to the source
	 * @param translationBundle
	 */
	private static void writeTranslationBundle(TranslationBundle translationBundle) {
		if (translationBundle != null) {
			String className = translationBundle.getClassName();
			String propertySufix = sourceFolder.getAbsolutePath() + File.separator + className.replace(".", File.separator);
			if (!translationBundle.getTranslations().isEmpty()) {
				Translation transl = translationBundle.getTranslations().values().iterator().next();
				for (String language : transl.getTranslations().keySet()) {
					
					String propertyFileName = propertySufix + (language.equals("") ? "" : "_" + language) + ".properties";
					Properties originalTranslationsMap = getOriginalPropertyTranslationsMap(propertyFileName);
					Map<String, String> translationsMap = new LinkedHashMap<String, String>();
					for (Translation translation : translationBundle.getTranslations().values()) {
						String key = translation.getKey();
						String value = translation.getTranslations().get(language);
						translationsMap.put(key, value);
					}
					originalTranslationsMap.putAll(translationsMap);
					
					Writer writer = null;
					try {
						writer = new OutputStreamWriter(new FileOutputStream(propertyFileName), "UTF-8");
						originalTranslationsMap.store(writer,"");
					} catch (Exception e) {
						throw new BuildException(e);
					} finally {
						if (writer != null)
							try {
								writer.close();
							} catch (IOException e) {
								throw new BuildException(e);
							}
					}
					
				} // end for languages
				
			} //end if translationList null
			
		}
	}
	
	/**
	 * This method gets all the properties from the properties file
	 * @param propertyFilePath
	 * @return
	 */
	private static Properties getOriginalPropertyTranslationsMap(String propertyFilePath) {
		Properties properties = new Properties();
		try {
			properties.load(new InputStreamReader(new FileInputStream(propertyFilePath), "UTF-8"));
			return properties;
		} catch (Exception e) {
			throw new BuildException(e);
		} 	
	}

}
