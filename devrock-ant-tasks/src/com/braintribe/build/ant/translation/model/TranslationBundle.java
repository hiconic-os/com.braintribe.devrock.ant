// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;

public class TranslationBundle {
	
	private String className;
	
	private Map<String, Translation> translations = new LinkedHashMap<String, Translation>();
	
	private Set<String> existingProperties = new HashSet<String>();

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	public Set<String> getExistingProperties() {
		return existingProperties;
	}

	public Map<String, Translation> getTranslations() {
		return translations;
	}
	
	public Translation getTranslation(String key) {
		return translations.get(key);
	}
	
	public Translation aquireTranslation(String key) {
		Translation translation = translations.get(key);
		if (translation == null) {
			translation = new Translation();
			translation.setKey(key);
			translations.put(key, translation);
		}
		return translation;
	}

	public void addTranslationsFromJavaFile(File sourceFile) {
		String filePath = sourceFile.getAbsolutePath();
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
				Translation translation = aquireTranslation(key);
				Map<String, String> translations = new LinkedHashMap<String, String>();
				for (PropertyContents propertyContents : propertyContentsList) {
					String languageName = propertyContents.getLanguageName();
					String value = propertyContents.getTranslationsMap().get(key);
					if (value != null) 
						translations.put(languageName, value);
				}
				translation.setTranslations(translations);
			}
		}
	}
	
	public void addTranslationsFromPropertyFile(File sourceFile) {
		String fileName = sourceFile.getName();
		fileName = fileName.substring(0, fileName.indexOf(".properties"));
		
		String filePath = sourceFile.getAbsolutePath();
		String folderPath = filePath.substring(0, filePath.lastIndexOf(fileName));
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
				Translation translation = aquireTranslation(key);
				Map<String, String> translations = new LinkedHashMap<String, String>();
				for (PropertyContents propertyContents : propertyContentsList) {
					String languageName = propertyContents.getLanguageName();
					String value = propertyContents.getTranslationsMap().get(key);
					if (value != null) 
						translations.put(languageName, value);
				}
				translation.setTranslations(translations);
			}
		}
	}
	
	/**
	 * This method returns the properties contents for a given property root
	 * @param propertyFilePath
	 * @return
	 */
	private static PropertyContents getPropertyContents(String propertyFilePath) {
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
	}
	
	/**
	 * This method gets all the properties from the properties file
	 * @param propertyFilePath
	 * @return
	 */
	private static Map<String, String> getOriginalPropertyTranslationsMap(String propertyFilePath) {
		Properties properties = new Properties();
		try {
			properties.load(new InputStreamReader(new FileInputStream(propertyFilePath), "UTF-8"));
			Map<String, String> map = new HashMap<String,String>();
			for (final String name: properties.stringPropertyNames())
			    map.put(name, properties.getProperty(name));
			return map;
		} catch (Exception e) {
			throw new BuildException(e);
		} 	
	}

	public void syncFrom(TranslationBundle translationBundle, SynchronizationParams params) {
		for (Translation translation: getTranslations().values()) {
			Translation otherTranslation = translationBundle.getTranslation(translation.getKey());
			
			if (otherTranslation != null) {
				translation.syncFrom(otherTranslation, params);
			}
		}
		
	}


}
