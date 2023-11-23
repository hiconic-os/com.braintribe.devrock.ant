// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.tools.ant.BuildException;

import com.braintribe.build.ant.utils.SingleQuoteCorrectionReader;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

public class Model {
	
	private static final String PROPERTIES_EXTENSION = ".properties";
	private Map<String, ArtifactBundle> artifactBundles = new LinkedHashMap<String, ArtifactBundle>();

	public Map<String, ArtifactBundle> getArtifactBundles() {
		return artifactBundles;
	}
	
	public ArtifactBundle getArtifactBundle(String artifactName) {
		return artifactBundles.get(artifactName);
	}
	
	public ArtifactBundle aquireArtifactBundle(String artifactName) {
		ArtifactBundle artifactBundle = artifactBundles.get(artifactName);
		if (artifactBundle == null) {
			artifactBundle = new ArtifactBundle();
			artifactBundle.setArtifactName(artifactName);
			artifactBundles.put(artifactName, artifactBundle);
		}
		return artifactBundle;
	}
	
	public void addBundlesFromSource(String artifactName, File sourceFolder) {
		ArtifactBundle artifactBundle = aquireArtifactBundle(artifactName);
		artifactBundle.addTranslationBundlesFromSource(sourceFolder);
	}
	
	public void updateSourceFromModel(String sourcePath, String groupId, String artifactId, String version) {
		String artifactName = groupId + ":" + artifactId + "-" + version;
		
		if (!sourcePath.endsWith(File.separator)) {
			sourcePath = sourcePath.concat(File.separator);
		}
		
		ArtifactBundle artifactBundle = getArtifactBundle(artifactName);
		
		if (artifactBundle != null) {
			for (TranslationBundle translationBundle: 
				artifactBundle.getTranslationBundles().values()) {

				Map<String, Properties> propertiesMap = new HashMap<String, Properties>();
				
				for (Translation translation: translationBundle.getTranslations().values()) {
					String key = translation.getKey();
					System.out.println("writing locales: "+ translation.getTranslations().keySet());
					for (Map.Entry<String, String> entry: translation.getTranslations().entrySet()) {
						String locale = entry.getKey();
						String value = entry.getValue();
						
						Properties properties = propertiesMap.get(locale);
						if (properties == null) {
							properties = new Properties();
							propertiesMap.put(locale, properties);
						}
						
						properties.put(key, value);
					}
				}
				
				String className = translationBundle.getClassName();
				
				for (Map.Entry<String, Properties> entry: propertiesMap.entrySet()) {
					String locale = entry.getKey();
					Properties properties = entry.getValue();
					
					String fileName = getPropertyFileName(className, locale);
					
					File directory = new File(sourcePath);
					File file = new File(directory, fileName);

					try {
						File parentDir = file.getParentFile();
						if (!parentDir.exists()) 
							parentDir.mkdirs();
						
						System.out.println("writing property file: " + file);
						
						Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
						properties.store(writer, "");
					} catch (Exception e) {
						throw new BuildException("error while writing sychronized text for className="+className+
								" locale="+locale, e);
					}
				}
			}
		}
	}
	
	public static String getPropertyFileName(String className, String locale) {
		StringBuilder fileName = new StringBuilder();
		if (className.endsWith(PROPERTIES_EXTENSION)) { //Class is already the property file
			if (locale != null && locale.trim().length() > 0) {
				String nameWithoutExtension = className.substring(0, className.indexOf(PROPERTIES_EXTENSION));
				fileName.append(nameWithoutExtension);
				fileName.append("_").append(locale);
				fileName.append(PROPERTIES_EXTENSION);
			} else {
				fileName.append(className);
			}
		} else {
			int index = className.lastIndexOf(".");
			
			String packageName = index != -1 ? className.substring(0, index): "";
			String simpleClassName = index != -1 ? className.substring(index + 1): className;
			
			String folder = packageName.replace('.', File.separatorChar);
			if (folder.length() > 0) {
				fileName.append(folder + File.separatorChar);
			}
			
			fileName.append(simpleClassName);
			
			if (locale != null && locale.trim().length() > 0) {
				fileName.append("_" + locale);
			}
			
			fileName.append(PROPERTIES_EXTENSION);
		}
		return fileName.toString();
	}
	
	/**
	 * This method prepares a Model from the source files.
	 * @param sourcePath
	 * @param groupId
	 * @param artifactId
	 * @param version
	 * @return
	 */
	public void updateModelFromSourcePath(String sourcePath, String groupId, String artifactId, String version) {
		String artifactName = groupId + ":" + artifactId + "-" + version;
		
		if (!sourcePath.endsWith(File.separator)) {
			sourcePath = sourcePath.concat(File.separator);
		}
		
		if (isValidSourcePath(sourcePath)) {
			addBundlesFromSource(artifactName, new File(sourcePath));
		}
	}
	
	/**
	 * This method verifies if the source path exists and is a directory
	 * @param sourcePath
	 */
	private static boolean isValidSourcePath(String sourcePath) {
		File sourceFolder = new File(sourcePath);
		
		return sourceFolder.exists() && sourceFolder.isDirectory(); 
	}
	
	public Set<String> scanForUsedLanguages() {
		Set<String> languages = new LinkedHashSet<String>();
		for (ArtifactBundle artifactBundle: artifactBundles.values()) {
			
			for (TranslationBundle translationBundle: 
				artifactBundle.getTranslationBundles().values()) {
				
				for (Translation translation: 
					translationBundle.getTranslations().values()) {
					
					languages.addAll(
							translation.getTranslations().keySet());
					
				}
				
			}
			
		}
		
		return languages;
	}

	/**
	 * This method writes a Model to a csv file
	 * @param csvPath
	 */
	public void writeModeltoCsv(String csvPath) {
		CSVWriter writer = null;
		try {
			Set<String> languages = scanForUsedLanguages();
			
			
			writer = new CSVWriter(
					new OutputStreamWriter(
							new FileOutputStream(csvPath), "UTF-8"), 
					CSVWriter.DEFAULT_SEPARATOR);
			
			// write the header
			List<String> csvHeader = new ArrayList<String>();
			csvHeader.add("artifact");
			csvHeader.add("class");
			csvHeader.add("key");
			csvHeader.addAll(languages);
			writer.writeNext(csvHeader.toArray(new String[csvHeader.size()]));
			
			// write the entries
			for (ArtifactBundle artifactBundle : artifactBundles.values()) {
				writeArtifactBundle(languages, artifactBundle, writer);
			}
		} catch (IOException e) {
			throw new BuildException();
		} finally {
			try {
				if (writer != null) writer.close();
			} catch (IOException e) {
				throw new BuildException(e);
			}
		}
	}
	
	/**
	 * Writes an ArtifactBundle to the csv file
	 * @param artifactBundle
	 * @param writer
	 */
	private static void writeArtifactBundle(Set<String> languages, ArtifactBundle artifactBundle, CSVWriter writer) {
		if (artifactBundle != null) {
			String artifactName = artifactBundle.getArtifactName();
			for (TranslationBundle translationBundle : artifactBundle.getTranslationBundles().values()) {
				writeTranslationBundle(languages, translationBundle, artifactName, writer);
			}
		}
	}
	/**
	 * Writes a TranslationBundle to the csv file
	 * @param translationBundle
	 * @param artifactName
	 * @param writer
	 */
	private static void writeTranslationBundle(Set<String> languages, TranslationBundle translationBundle, String artifactName, CSVWriter writer) {
		if (translationBundle != null) {
			String className = translationBundle.getClassName();
			for (Translation translation : translationBundle.getTranslations().values()) {
				writeTranslation(languages, translation, className, artifactName, writer);
			}
		}
	}
	
	/**
	 * Writes a Translation to the csv file
	 * @param translation
	 * @param className
	 * @param artifactName
	 * @param writer
	 */
	private static void writeTranslation(Set<String> languages, Translation translation, String className, String artifactName, CSVWriter writer) {
		if (translation != null && translation.getTranslations() != null) {
			String key = translation.getKey();
			
			List<String> entriesToWrite = new ArrayList<String>();
			entriesToWrite.add(artifactName);
			entriesToWrite.add(className);
			entriesToWrite.add(key);
			
			Map<String, String> translations = translation.getTranslations();
			
			for (String language: languages) {
				String localizedText = translations.get(language);
				if (localizedText == null) 
					localizedText = "";
				entriesToWrite.add(localizedText);
			}
			
			writer.writeNext(entriesToWrite.toArray(new String[entriesToWrite.size()]));
		}
	}
	
	protected Translation aquireTranslation(String artifact, String className, String key) {
		return aquireArtifactBundle(artifact)
		.aquireTranslationBundle(className)
		.aquireTranslation(key);
	}

	/**
	 * This method creates a Model from the csv file
	 * @param csvPath
	 * @return
	 */
	public void updateModelFromCsv(String csvPath) {
		File csvFile = new File(csvPath);
		if (!csvFile.exists()) return;
		
		CSVReader reader = null;
		try {
			reader = new CSVReader(new SingleQuoteCorrectionReader(new BufferedReader(new InputStreamReader(new FileInputStream(csvFile), "UTF-8"))));
			String[] csvHeader = reader.readNext();
			CsvRowAccessor csvRowAccessor = new CsvRowAccessor(csvHeader);
			Collection<String> languages = csvRowAccessor.getLanguages();
			
			String [] nextLine;
		    while ((nextLine = reader.readNext()) != null) {
		    	String artifact = csvRowAccessor.getArtifact(nextLine);
		    	String className = csvRowAccessor.getClassname(nextLine);
		    	String key = csvRowAccessor.getKey(nextLine);
		    	
		    	Translation translation = aquireTranslation(artifact, className, key);

		    	Map<String, String> localizedValues = translation.getTranslations();
		    	
		    	for (String language: languages) {
		    		String localizedValue = csvRowAccessor.getLocalizedValue(nextLine, language);
		    		if (localizedValue.length() != 0) {
		    			localizedValues.put(language, localizedValue);
		    		}
		    	}
		    } //end while
		    
		} catch (Exception e) {
			throw new BuildException(e);
		} finally {
			try {
				if (reader != null) reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static class CsvRowAccessor {
		private int artifactColumn;
		private int classNameColumn;
		private int keyColumn;
		private Map<String, Integer> languageColumns = new LinkedHashMap<String, Integer>();
		
		public CsvRowAccessor(String[] headerRow) {
			int index = 0;
			for (String header: headerRow) {
				if (header.equals("artifact")) {
					artifactColumn = index;
				}
				else if (header.equals("class")) {
					classNameColumn = index;
				}
				else if (header.equals("key")) {
					keyColumn = index;
				}
				else {
					// must be a language column
					languageColumns.put(header, index);
				}
				
				index++;
			}
		}
		
		public String getArtifact(String[] row) {
			return row[artifactColumn];
		}
		
		public String getClassname(String[] row) {
			return row[classNameColumn];
		}
		
		public String getKey(String[] row) {
			return row[keyColumn];
		}
		
		public Collection<String> getLanguages() {
			return languageColumns.keySet();
		}
		
		public String getLocalizedValue(String[] row, String language) {
			int index = languageColumns.get(language);
			return row[index];
		}
	}
	
	public void syncFrom(Model model, SynchronizationParams params) {
		for (ArtifactBundle artifactBundle: getArtifactBundles().values()) {
			String artifactName = artifactBundle.getArtifactName();
			ArtifactBundle otherArtifactBundle = model.getArtifactBundle(artifactName);
			if (otherArtifactBundle != null) {
				artifactBundle.syncFrom(otherArtifactBundle, params);
			}
		}
	}
}
