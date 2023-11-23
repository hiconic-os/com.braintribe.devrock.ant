package com.braintribe.build.ant.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.braintribe.exception.Exceptions;

public class XmlUtils {
	public static void writeXml(final Node node, Writer writer) {
		try {
			final DOMImplementationLS impl = (DOMImplementationLS)  DOMImplementationRegistry.newInstance().getDOMImplementation("LS");

			final LSSerializer lsSerializer= impl.createLSSerializer();
			// see https://xerces.apache.org/xerces2-j/javadocs/api/org/w3c/dom/ls/LSSerializer.html#getDomConfig()
			// lsSerializer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
			lsSerializer.getDomConfig().setParameter("xml-declaration", Boolean.FALSE);
			lsSerializer.getDomConfig().setParameter("comments", Boolean.TRUE);

			final LSOutput destination = impl.createLSOutput();
			destination.setEncoding("UTF-8");
			destination.setCharacterStream(writer);
			
			lsSerializer.write(node, destination);
			writer.write("\n");
		} catch (Exception e) {
			throw Exceptions.unchecked(e);
		}
	}
		
	public static void writeXml(File file, Document document) {
		try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
			writeXml(document, writer);
		}
		catch (Exception e) {
			throw Exceptions.unchecked(e);
		}
	}
	
}
