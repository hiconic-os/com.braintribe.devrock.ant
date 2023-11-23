// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * This ant task is able to update the .classpath file of an eclipse
 * project. It can be used to bring the transitive dependencies of 
 * a pom.xml with sources to the eclipse project
 * 
 * @author Dirk
 *
 */
public class UpdateEclipseClasspath extends Task {
	private static final TransformerFactory poxTranformerFactory = TransformerFactory.newInstance();
	private String sourceFileSetId; 
	private String fileSetId; 
	private boolean sourcesNeeded = false;
	private String repoDir = System.getProperty("user.home") + "/.m2/repository";
	private String classpathVar = "M2_REPO";
	
	public void setClasspathVar(String classpathVar) {
		this.classpathVar = classpathVar;
	}
	
	public void setSourcesNeeded(boolean sourcesNeeded) {
		this.sourcesNeeded = sourcesNeeded;
	}

	public void setSourceFileSetId(String sourceFileSetId) {
		this.sourceFileSetId = sourceFileSetId;
	}
	
	public void setFileSetId(String fileSetId) {
		this.fileSetId = fileSetId;
	}
	
	public void setRepoDir(String repoDir) {
		this.repoDir = repoDir;
	}
	
	private Set<File> getFiles(String id) {
		if (id == null) return Collections.emptySet();
		FileSet fileSet = (FileSet)getProject().getReference(id);
		if (fileSet == null) return Collections.emptySet();
		
		fileSet.setProject(getProject());
		StringTokenizer tokenizer = new StringTokenizer(fileSet.toString(), ";");
		
		Set<File> files = new HashSet<File>();
		File dir = fileSet.getDir(getProject());
		if (dir.equals(new File(repoDir)))
			dir = new File(classpathVar);
		
		while (tokenizer.hasMoreTokens()) {
			File file = new File(dir, tokenizer.nextToken());
			files.add(file);
		}
		
		return files;
	}
	
	private File makeSourceFile(File file) {
		String name = file.getName();
		
		int index = name.lastIndexOf('.');
		
		if (index != -1) {
			String base = name.substring(0, index);
			String ext = name.substring(index);
			name = base + "-sources" + ext;
			return new File(file.getParent(), name);
		}
		else return file;
	}
	
	@Override
	public void execute() throws BuildException {
		Set<File> sourceFiles = getFiles(sourceFileSetId);
		Set<File> files = getFiles(fileSetId);
		
		// create normal classpath entries
		Set<ClasspathEntry> entries = new TreeSet<ClasspathEntry>(new Comparator<ClasspathEntry>() {
			@Override
			public int compare(ClasspathEntry o1, ClasspathEntry o2) {
				String s1 = o1.getPath().getName();
				String s2 = o2.getPath().getName();
				return s1.compareToIgnoreCase(s2);
			}
		});
		for (File file: files) {
			File source = makeSourceFile(file);
			if (!sourceFiles.contains(source))
				source = null;
			
			ClasspathEntry entry = new ClasspathEntry(file, source);
			entries.add(entry);
		}
		
		// create extra source classpath entries
		if (sourcesNeeded) {
			for (File file: sourceFiles) {
				ClasspathEntry entry = new ClasspathEntry(file);
				entries.add(entry);
			}
		}
		
		updateClasspathFile(entries);
	}
	
	protected void updateClasspathFile( Set<ClasspathEntry> entries) throws BuildException {
		updateClasspathFile(entries, ".classpath");
	}
	
	protected void updateClasspathFile(Set<ClasspathEntry> entries, String fileName) throws BuildException {
		try {
			File classpathFile = new File(getProject().getBaseDir(), fileName);
			Document doc = null;
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			if (classpathFile.exists()) {				
				doc = builder.parse(classpathFile);
			} else {
				doc = builder.newDocument();				
				doc.appendChild( doc.createElement("classpath"));
			}

			Element rootElement = doc.getDocumentElement();
			
			// remove repo vars
			NodeList nodes = rootElement.getChildNodes();
			
			for (int i = nodes.getLength() -1; i >= 0; i--) {
				Node node = nodes.item(i);
				if (node instanceof Text) rootElement.removeChild(node);
				if (node instanceof Element) {
					Element element = (Element)node;
					String kind = element.getAttribute("kind");
					String path = element.getAttribute("path");
					
					if (kind.equals("var") && path.startsWith(classpathVar)) {
						rootElement.removeChild(element);
					}
				}
			}
			
			// add vars
			for (ClasspathEntry entry: entries) {
				Element element = doc.createElement("classpathentry");
				element.setAttribute("kind", "var");
				element.setAttribute("path", entry.getPath().toString().replace('\\', '/'));
				File sourcePath = entry.getSourcePath();
				if (sourcePath != null)
					element.setAttribute("sourcepath", sourcePath.toString().replace('\\', '/'));
				rootElement.appendChild(element);
			}
			
			DOMSource domSource = new DOMSource(doc);
			StreamResult streamResult = new StreamResult(classpathFile);
			 
			//TODO: optionally use shared serializer...
			Transformer serializer; 
			synchronized (poxTranformerFactory) {
				// DEACTIVATED: poxTranformerFactory.setAttribute("indent-number", new Integer(2));
				serializer = poxTranformerFactory.newTransformer();
			}
			 
			//TODO: take properties from parameter
			serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");
			 
			serializer.transform(domSource, streamResult); 
		} catch (Exception e) {
			throw new BuildException("error while updating eclipse .classpath file", e);
		}
	}
	
	protected static class ClasspathEntry {
		private File path;
		private File sourcePath;
		
		public ClasspathEntry(File path) {
			this(path, null);
		}
		
		public ClasspathEntry(File path, File sourcePath) {
			super();
			this.path = path;
			this.sourcePath = sourcePath;
		}
		
		public File getPath() {
			return path;
		}
		
		public File getSourcePath() {
			return sourcePath;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("[path='" );
			builder.append(path);
			builder.append("'");
			if (sourcePath != null) {
				builder.append(" sourcepath='");
				builder.append(sourcePath);
				builder.append("'");
			}
			builder.append("]");
				
			return builder.toString();
		}
	}
	
}
