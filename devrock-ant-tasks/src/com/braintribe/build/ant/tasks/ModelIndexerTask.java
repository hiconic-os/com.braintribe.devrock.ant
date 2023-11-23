// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.tasks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.resources.FileResource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.logging.Logger;
import com.braintribe.model.artifact.processing.version.VersionMetricTuple;
import com.braintribe.model.artifact.processing.version.VersionProcessingException;
import com.braintribe.model.artifact.processing.version.VersionProcessor;
import com.braintribe.model.artifact.version.Version;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

/**
 *
 * A task to create the index file for the model synchronizer
 * 
 * parameters<br/>
 *  fileset : a {@link FileSet} containing {@link FileResource} that point to the pom files (as collected by the bt:dependencies task)<br/>
 *  indexFile : a {@link File} where the index data should be written to.<br/>
 *  <br/>
 *  the task reads the pom file as simple DOM and accesses the groupId, artifactId and version of the artifact declaration,
 *  then it reads the properties section to retrieve the model-revision value.<br/>
 *  if the version of the pom already contains a revision, this is used. 
 *  otherwise, a revision is added 
 *  <br/>
 *  to debug: 
 *	set ANT_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=y
 *
 * @author Pit
 *
 */
public class ModelIndexerTask extends Task {
	private static Logger log = Logger.getLogger(ModelIndexerTask.class);
	
	private FileSet pomFileSet;
	private File indexFile;

	@Required @Configurable
	public void addFileset(FileSet pomFileSet) {
		this.pomFileSet = pomFileSet;
	}
	
	@Required @Configurable
	public void setIndexFile(File indexFile) {
		this.indexFile = indexFile;
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.Task#execute()
	 */
	@Override
	public void execute() throws BuildException {
		if (pomFileSet == null) {
			throw new BuildException( String.format("no file set for poms passed"));
		}
		if (indexFile == null) {
			throw new BuildException( String.format("index file may not be null"));
		}
			
		new ColorSupport(getProject()).installConsole();
	
		List<String> lines = new ArrayList<String>();
	
			
			
		Iterator<Resource> iterator =  pomFileSet.iterator();
		
		while (iterator.hasNext()) {
			FileResource resource = (FileResource) iterator.next();
			File file = resource.getFile();
			FileInputStream stream = null;
			try {
				stream = new FileInputStream(file);
				Document document = DomParser.load().from( stream);
				// extract artifact information 
				Element parent = document.getDocumentElement();
				String groupId = DomUtils.getElementValueByPath( parent, "groupId", false);
				if (groupId == null) {
					groupId = DomUtils.getElementValueByPath(parent, "parent/groupId", false);
					if (groupId == null) {
						log.error( String.format("no group-id found in file [%s] (neither directly nor via parent reference)", file.getAbsolutePath()));
					}
				}
				String artifactId = DomUtils.getElementValueByPath( parent, "artifactId", false);
				if (artifactId == null) {
					log.error( String.format("no artifact-id found in file [%s]", file.getAbsolutePath()));
				}
				// manage versions 
				String versionAsString = DomUtils.getElementValueByPath( parent, "version", false);
				if (versionAsString == null) {
					log.error( String.format("no version found in file [%s]", file.getAbsolutePath()));
				}
				Version mcVersion;
				try {
					mcVersion = VersionProcessor.createFromString(versionAsString);
				} catch (VersionProcessingException e) {
					String msg = String.format("cannot build a valid version from string [%s] as defined in file [%s]", versionAsString, file.getAbsolutePath());
					throw new BuildException( msg, e);
				}
				VersionMetricTuple metric;
				try {
					metric = VersionProcessor.getVersionMetric(mcVersion);
				} catch (VersionProcessingException e) {
					String msg = String.format("cannot extract a valid metric from version [%s] as defined in file [%s]", versionAsString, file.getAbsolutePath());
					throw new BuildException( msg, e);
				}
				String line;
				if (metric.revision != null && metric.revision != 0) { // revision direct in version 					
					line = String.format("%s:%s#%s,%s-%s-model.xml,%s", groupId, artifactId, versionAsString, artifactId, versionAsString, metric.revision);
				} 
				else {												
					// extract revision from pom property section, if any 					
					String revision = DomUtils.getElementValueByPath(parent, "properties/model-revision", false);
					if (revision == null) {
						line = String.format("%s:%s#%s,%s-%s-model.xml", groupId, artifactId, versionAsString, artifactId, versionAsString);
					} else {
						line = String.format("%s:%s#%s,%s-%s-model.xml,%s", groupId, artifactId, versionAsString, artifactId, versionAsString, revision);
					}
				}
				log.debug( line);
				lines.add( line);
				
				
				
			} catch (FileNotFoundException e) {
				log.error(String.format("cannot read file [%s]", file.getAbsolutePath()), e);
			} catch (DomParserException e) {
				log.error(String.format("cannot read pom [%s]", file.getAbsolutePath()), e);
			}
			finally {
				if (stream != null)
					try {
						stream.close();
					} catch (IOException e) {
						// ignore
					}
			}
		}
		FileOutputStream outStream = null;
		try {
			Collections.sort(lines);
			outStream = new FileOutputStream(indexFile);
			boolean first = true;
			for (String line : lines) {
				try {
					if (!first)
						line = "\n" + line;
					first = false;
					outStream.write( line.getBytes());
				} catch (IOException e) {
					log.error(String.format("cannot write index of pom to index [%s]", indexFile.getAbsolutePath()), e);
				}
			}
		}
		catch (FileNotFoundException e1) {
			throw new BuildException( String.format("cannot write to index file [%s]", indexFile.getAbsolutePath()));
		}
		finally {
			if (outStream != null) {
				try {
					outStream.flush();
					outStream.close();
				} catch (IOException e) {
					log.error(String.format("cannot close outputstream to [%s]", indexFile.getAbsolutePath()), e);
				}
			}
		}
		
		super.execute();
	}
	

}
