// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================



package com.braintribe.build.process.repository.process.svn;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.braintribe.build.process.ProcessException;
import com.braintribe.build.process.ProcessExecution;
import com.braintribe.build.process.ProcessResults;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;
import com.braintribe.logging.Logger;
import com.braintribe.utils.xml.dom.DomUtils;
import com.braintribe.utils.xml.parser.DomParser;

public class SvnUtil {
	
	private static Logger log = Logger.getLogger(SvnUtil.class);
	
	private static int maxRetries = 2;
	
	public static Document list(ProcessNotificationListener listener, String url) throws SourceRepositoryAccessException {
		//String pattern = "svn list --xml \"{0}\"";
		//String cmd = MessageFormat.format(pattern, url);
		String[] cmd = {"svn", "list", "--xml", url};
		
		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			try {
				return DomParser.load().from(svnOutput.getNormalText());
			} catch (Exception e) {
				throw new SourceRepositoryAccessException("error while parsing result xml", e);
			}
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
		
	}
	
	public static String cat( ProcessNotificationListener listener, String url) throws SourceRepositoryAccessException {
		String[] cmd = {"svn", "cat", url};
		
		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return svnOutput.getNormalText();
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
	}
	
	public static void del(ProcessNotificationListener listener, String url, String message) throws SourceRepositoryAccessException {
		//String pattern = "svn del \"{0}\" -m \"{1}\"";
		//String cmd = MessageFormat.format(pattern, url, message);
		String[] cmd = {"svn", "del", url, "-m", message};
		
		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
		
	}
	public static void export(ProcessNotificationListener listener, String fromUrl, File toFile) throws SourceRepositoryAccessException {
		//String pattern = "svn export \"{0}\" \"{1}\"";
		//String cmd = MessageFormat.format(pattern, fromUrl, toFile.toString());
		String[] cmd = {"svn", "export", fromUrl, toFile.toString()};

		ProcessResults svnOutput = process(listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
	}

	
	public static void exportForced(ProcessNotificationListener listener, String fromUrl, File toFile) throws SourceRepositoryAccessException {
		//String pattern = "svn export \"{0}\" \"{1}\" --force";
		//String cmd = MessageFormat.format(pattern, fromUrl, toFile.toString());
		String[] cmd = {"svn", "export", fromUrl, toFile.toString(), "--force"};

		ProcessResults svnOutput = process(listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
	}
	
	public static void checkout(ProcessNotificationListener listener, String fromUrl, File toFile, boolean recursive) throws SourceRepositoryAccessException {
		//String pattern = "svn co \"{0}\" \"{1}\" {2}";
		String recursiveOption = recursive? "": "-N";
		//String cmd = MessageFormat.format(pattern, fromUrl, toFile.toString(), recursiveOption);
		String[] cmd = {"svn", "co", fromUrl, toFile.toString(), recursiveOption};

		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
	}
	
	public static void commit(ProcessNotificationListener listener, File toFile, String message) throws SourceRepositoryAccessException {
		//String pattern = "svn commit \"{0}\" -m \"{1}\"";
		//String cmd = MessageFormat.format(pattern, toFile, message);
		if (message == null)
			message = "sincere, but empty filler";
		
		String[] cmd = {"svn", "commit", toFile.toString(), "-m", message};
		
		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}		
	}
	
	public static void svn_import(ProcessNotificationListener listener, File toFile, String url, String msg) throws SourceRepositoryAccessException {
		//String pattern = "svn import \"{0}\" \"{1}\" -m\"{2}\"";

		if (msg == null)
			msg = "empty, but sincere filler";
		String[] cmd = {"svn", "import", toFile.toString(), url, "-m", msg};
		
		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
		
	}
	
	/**
	 * adds a file to svn  
	 */
	public static void add( ProcessNotificationListener listener, File toFile) throws SourceRepositoryAccessException {
		String[] cmd = {"svn", "add", toFile.toString(), "--non-interactive"};
		
		ProcessResults svnOutput = process(listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
			
		
	}

	
	public static File exportToTemporaryFile(ProcessNotificationListener listener, String fromUrl) throws SourceRepositoryAccessException {
		try {
			File file = File.createTempFile("svn-export", null);
			exportForced(listener, fromUrl, file);
			return file;
		} catch (IOException e) {
			throw new SourceRepositoryAccessException(e);
		}
	}
	
	public static void update(ProcessNotificationListener listener, File file, boolean recursive) throws SourceRepositoryAccessException {
		//String pattern = "svn up \"{0}\" \"{1}\"";
		String recursiveOption = recursive? "": "-N";
		//String cmd = MessageFormat.format(pattern, recursiveOption, file.toString());
		String[] cmd = {"svn", "up", recursiveOption, file.toString(), "--non-interactive"};
		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}		
	}
	
	/**
	 * copies everything from fromUrl to toUrl. 
	 * WARNING: make sure toUrl doesn't exist. If it does, you must use branchContentsOnly 
	 */
	public static void branch(ProcessNotificationListener listener, String fromUrl, String toUrl, String message) throws SourceRepositoryAccessException {
		//String cmdPattern = "svn copy \"{0}\" \"{1}\" -m \"{2}\"";
		//String cmd = MessageFormat.format(cmdPattern, fromUrl, toUrl, message);
		String[] cmd = {"svn", "copy", fromUrl, toUrl, "-m", message, "--non-interactive"};

		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
	}
	
	/**
	 * copies everything from fromUrl to toUrl.
	 * WARNING: make sure toUrl does exist. If it doesn't, you must either use branch (anyway,
	 * it's way faster) or mkdir. 	
	 */
	public static void branchContentsOnly(ProcessNotificationListener listener, String fromUrl, String toUrl, String message) throws SourceRepositoryAccessException {
		Document doc = list( listener, fromUrl);
		NodeList nodes = doc.getElementsByTagName( "entry");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element entryE = (Element) nodes.item(i);
			Element nameE = DomUtils.getElementByPath(entryE, "name", false);
			String urlToUse = fromUrl + "/" + nameE.getTextContent();
			branch( listener, urlToUse, toUrl, message);
		}
	}
	
	public static void ensurePath(ProcessNotificationListener listener, String url, String message) throws SourceRepositoryAccessException {
		//String cmdPattern = "svn mkdir \"{0}\" -m \"{1}\"";
		//String cmd = MessageFormat.format(cmdPattern, url, message);
		String[] cmd = {"svn", "mkdir", url, "-m", message, "--non-interactive"};

		ProcessResults svnOutput = process( listener, cmd);
		
		if (svnOutput.getRetVal() == 0) {
			return;
		}
		else {
			throw new SourceRepositoryAccessException(svnOutput.getErrorText());
		}
	}
	
	public static void mkdir(ProcessNotificationListener listener, String url, String message) throws SourceRepositoryAccessException {
		//String cmdPattern = "svn mkdir \"{0}\" -m \"{1}\"";
		//String cmd = MessageFormat.format(cmdPattern, url, message);
		String[] cmd = {"svn", "mkdir", url, "-m", message, "--non-interactive"};
		try {
			ProcessResults svnOutput = ProcessExecution.runCommand(listener, null, cmd);
			
			if (svnOutput.getRetVal() == 0) {
				return;
			}
			else {
				throw new SourceRepositoryAccessException(svnOutput.getErrorText());
			}
		} catch (ProcessException e) {
			throw new SourceRepositoryAccessException( "cannot run process as " + e, e);
		}
	}

	/**
	 * processing function with inbuilt retries
	 * 
	 * @param cmd - the string arrays with the arguments 
	 * @return - the ProcessResults of the process 
	 * @throws SourceRepositoryAccessException - anything goes wrong.. 
	 */
	private static ProcessResults process( ProcessNotificationListener listener, String [] cmd) throws SourceRepositoryAccessException{
		  
		  int retry = 0;
		  
		  ProcessResults svnOutput;
		  while (true) {
		   try {
			  svnOutput = ProcessExecution.runCommand(listener, null, cmd);
		   } catch (ProcessException e) {
			  String msg = "cannot run process as " + e;
			  log.error( msg, e);
			  throw new SourceRepositoryAccessException( msg, e);
		   }
		   
		   if (svnOutput.getRetVal() == 0) {
		    return svnOutput;
		   } else {
		    if (svnOutput.getErrorText().contains("E175002:")) {
		    	 retry++;		     
			     if (retry <= maxRetries) {
			    	 String msg = "connection problem with SVN retrying...";
			    	 System.err.println( msg);
			    	 log.warn( msg);
			     } else {
			    	 String msg = "connection problem with SVN. max retries reached";			    	 
			    	 System.err.println( msg);
			    	 log.warn( msg);
			    	 break;
			     }
			     
		    } else
		    	break;
		   }
		  }
		  throw new SourceRepositoryAccessException( svnOutput.getErrorText());
	}
	
	
	/**
	 * not very clear what svn returns here.
	 * Currently it returns 1 which leads to SvnInfo throwing an exception.
	 * But that is prone to change - currently it is flux, and depends on svn / client 
	 * versions.. this function should handle most of the cases 
	 */
	public static boolean exists(ProcessNotificationListener listener, String url) {
		SvnInfo svnInfo;
		try {
			svnInfo = new SvnInfo();
			svnInfo.setListener(listener);
			svnInfo.read( url);
			
			return svnInfo.isValid();
		} catch (Exception e) { // currently SvnInfo throws an exception when encountering an invalid url.. 
			return false;
		}

	}
}
