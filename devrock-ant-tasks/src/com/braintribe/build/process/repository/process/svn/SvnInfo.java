// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process.repository.process.svn;

import java.io.File;
import java.io.StringReader;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import com.braintribe.build.process.ProcessException;
import com.braintribe.build.process.ProcessExecution;
import com.braintribe.build.process.ProcessResults;
import com.braintribe.build.process.listener.MessageType;
import com.braintribe.build.process.listener.ProcessNotificationListener;
import com.braintribe.build.process.repository.ProcessNotificationBroadcasterImpl;
import com.braintribe.build.process.repository.process.SourceRepositoryAccessException;
import com.braintribe.cfg.Configurable;

public class SvnInfo {
	private static final Pattern pathSplitPattern = Pattern.compile("/");
	private Document document;
	private ProcessNotificationListener listener;
	
	@Configurable
	public void setListener(ProcessNotificationListener listener) {
		this.listener = listener;
	}
		
	private static Element getFirstElement(Element parent, String tagName) {
		Node childNode = parent.getFirstChild();
		while (childNode != null) {
			if (childNode instanceof Element && 
				(tagName == null || childNode.getNodeName().equals(tagName)))
				return (Element)childNode;
			childNode = childNode.getNextSibling();
		}
		return null;
	}
	
	public static Element getElementByPath(Element parent, String path) {
		String tagNames[] = pathSplitPattern.split(path);
		Element element = parent;
		for (String tagName: tagNames) {
			element = getFirstElement(element, tagName);
			if (element == null) break;
		}
		
		return element;
	}
	
	public String getWcPath() {
		Element wcE = getElementByPath( document.getDocumentElement(), "entry/wc-info/wcroot-abspath");
		if (wcE != null)
			return wcE.getTextContent();
		return null;
	}


	public String getUrl() {
		Element element = getElementByPath(document.getDocumentElement(), "entry/url");
		if (element != null) return element.getTextContent();
		else return null;
	}
	
		
	public String getRevision() {
		Element element = getElementByPath(document.getDocumentElement(), "entry");
		if (element != null) 
			return element.getAttribute("revision");
		else 
			return null;
	}
	public boolean isValid() {
		return getUrl() != null;
	}
	
	public void read( File file) throws SourceRepositoryAccessException {
		read( file.getAbsolutePath());
	}
	
	public void read(String target) throws SourceRepositoryAccessException {
		//String cmdPattern = "svn info --xml \"{0}\"";
		//String cmd = MessageFormat.format(cmdPattern, target);
		String[] cmd = {"svn", "info", "--xml", target, "--non-interactive"};
		
		try {
			ProcessResults svnOutput = ProcessExecution.runCommand(listener, null, cmd);
			
			if (svnOutput.getRetVal() == 0) {
				try {
					DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					document = docBuilder.parse(new InputSource(new StringReader(svnOutput.getNormalText())));
				} catch (Exception e) {
					throw new SourceRepositoryAccessException(e);
				}
			}
			else {
				throw new SourceRepositoryAccessException(svnOutput.getErrorText());
			}
		} catch (ProcessException e) {
			throw new SourceRepositoryAccessException( "cannot run process as " + e, e);
		}
	}
	
	public static void main(String[] args) {
		/*
		File file = new File("/Users/micheldocouto/Work/Development/ClientGwt/artifacts-build/com/braintribe/gwt/ExtJsResources");
		//String url = "http://vie-svn/svn/svn-master/Development/artifacts/com/braintribe/utils/IoUtils";

		SvnInfo info = new SvnInfo(file);
		System.out.println(info.getUrl());
		*/
		ProcessNotificationListener listener = new ProcessNotificationListener() {
			
			@Override
			public void acknowledgeProcessNotification(MessageType messageType, String msg) {
				System.out.println( messageType.toString() + " : " + msg);				
			}
		};
		ProcessNotificationBroadcasterImpl broadcaster = new ProcessNotificationBroadcasterImpl();
		broadcaster.addListener(listener);
		try {			
			String url = "https://svn.bt.com/repo/master/Development/artifacts/com/braintribe/test/dependencies";			
			SvnInfo info = new SvnInfo();			
			info.setListener( broadcaster);
			info.read(url);
			System.out.println("Revision is [" + info.getRevision() + "]");
		} catch (SourceRepositoryAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
