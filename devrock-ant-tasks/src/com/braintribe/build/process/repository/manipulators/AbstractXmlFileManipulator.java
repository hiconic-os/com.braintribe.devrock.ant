// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository.manipulators;

import java.io.File;

import org.w3c.dom.Document;

import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;
import com.braintribe.logging.Logger;
import com.braintribe.utils.xml.parser.DomParser;
import com.braintribe.utils.xml.parser.DomParserException;

public abstract class AbstractXmlFileManipulator  {
	private static Logger log = Logger.getLogger(AbstractXmlFileManipulator.class);
	
	protected Document document = null;
	
	public void load(File file) throws SourceRepositoryAccessException {
		try {
			document = DomParser.load().from(file);
		}
		catch (DomParserException e) {
			String msg = "cannot load file [" + file.getAbsolutePath() + "]"; 
			log.error( msg, e);
			throw new SourceRepositoryAccessException( msg, e);
		}
	}
	
	public abstract void adjust();
	
	public void store(File file) throws SourceRepositoryAccessException {
		try {
			DomParser.write().from(document).setEncoding("UTF-8").to(file);
		} catch (DomParserException e) {
			String msg = "cannot store file [" + file.getAbsolutePath() + "]"; 
			log.error( msg, e);
			throw new SourceRepositoryAccessException( msg, e);
		}
	}
	
	public String getAdjustedDocumentAsString() throws SourceRepositoryAccessException {
		try {
			return DomParser.write().from(document).to();
		} catch (DomParserException e) {
			String msg = "cannot convert document [" + document + "] to String"; 
			log.error( msg, e);
			throw new SourceRepositoryAccessException( msg, e);
		}
	}
}
