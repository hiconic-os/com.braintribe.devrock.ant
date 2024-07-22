// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.braintribe.utils.paths.UniversalPath;

public class GenerateCopyrightNotice extends Task {
	
	private File noticeFile;

	private List<FileSet> sourceFileSets = new ArrayList<>();

    public void addSourceFileSet(FileSet fileSet) {
    	sourceFileSets.add(fileSet);
    }
	
	public void setNoticeFile(File noticeFile) {
		this.noticeFile = noticeFile;
	}

	@Override
	public void execute() throws BuildException {
		if (noticeFile == null)
			throw new BuildException("Missing noticeFile");
		
			
		CopyrightIndexer copyrightIndexer = new CopyrightIndexer();
		
		try {
			for (FileSet fileSet: sourceFileSets)
				copyrightIndexer.scanFileSet(fileSet);
			
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(noticeFile), StandardCharsets.UTF_8))) {
				writer.write("Detailed Copyright Information\n");
				
				for (Map.Entry<String, SortedMap<String, SortedSet<String>>> entry: copyrightIndexer.getCopyrights().entrySet()) {
					String copyright = entry.getKey();
					writer.write("\n");
					writer.write(copyright);
					writer.write("\n\n  affects the following classes:\n");
					
					SortedMap<String, SortedSet<String>> packageMap = entry.getValue();

					for (Map.Entry<String, SortedSet<String>> packageEntry: packageMap.entrySet()) {
						String packageName = packageEntry.getKey();
						SortedSet<String> classes = packageEntry.getValue();
						
						if (classes.size() == 1) {
							writer.write("\n  ");
							writer.write(packageName);
							writer.write(".");
							
							String clazz = classes.first();
							writer.write(clazz);
						}
						else {
							writer.write("\n  ");
							writer.write(packageName);
							writer.write(".\\");
							
							
							for (String clazz: classes) {
								writer.write("\n    ");
								writer.write(clazz);
							}
						}
						
					}
				}
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private class CopyrightIndexer {
		private SortedMap<String, SortedMap<String, SortedSet<String>>> copyrights = new TreeMap<>();
		
		public SortedMap<String, SortedMap<String, SortedSet<String>>> getCopyrights() {
			return copyrights;
		}
		
		public void scanFileSet(FileSet fileSet) throws IOException {
			// Retrieve files using DirectoryScanner
	        DirectoryScanner scanner = fileSet.getDirectoryScanner(getProject());
	        String[] includedFiles = scanner.getIncludedFiles();
	        
	        File baseDir = fileSet.getDir();

	        // Convert to a list of File objects
	        for (String filePath : includedFiles) {
	        	scanFile(new File(filePath), new File(baseDir, filePath));
	        }
		}
		
		private void scanFile(File relativeFile, File file) throws IOException {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.ISO_8859_1))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					line = line.trim();
					if (line.isEmpty())
						continue;
					
					if (!line.startsWith("//"))
						return;
					
					line = line.substring(2).trim();
					
					
					String className = UniversalPath.from(relativeFile).toDottedPath();
					className = className.substring(0, className.length() - ".java".length());
					
					int index = className.lastIndexOf(".");
					
					final String packageName;
					final  String simpleClassName;
					if (index == -1) {
						packageName = "<default>";
						simpleClassName = className;
					}
					else {
						packageName = className.substring(0, index);
						simpleClassName = className.substring(index + 1);
					}
					
					if (line.startsWith("Copyright ")) {
						SortedMap<String, SortedSet<String>> packageMap = copyrights.computeIfAbsent(line, k -> new TreeMap<>());
						
						SortedSet<String> classes = packageMap.computeIfAbsent(packageName, k -> new TreeSet<>());
						
						classes.add(simpleClassName);
						
						return;
					}
				}
			}
		}
	}
}
