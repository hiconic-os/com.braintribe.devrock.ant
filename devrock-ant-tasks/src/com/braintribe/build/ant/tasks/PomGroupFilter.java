// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.tasks;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;

import com.braintribe.utils.DOMTools;
import com.braintribe.utils.lcd.CollectionTools2;
import com.braintribe.utils.xml.XmlTools;

/**
 * @author peter.gazdik
 */
public class PomGroupFilter extends Task {

	private File sourcePom;
	private File targetPom;
	private Set<String> groups = Collections.singleton("default");

	public void setSourcePom(File source) {
		this.sourcePom = source;
	}

	public void setTargetPom(File target) {
		this.targetPom = target;
	}

	public void setGroups(String groups) {
		this.groups = CollectionTools2.asSet(groups.split(","));
	}

	@Override
	public void execute() throws BuildException {
		validate();

		Document document = loadSourcePom();
		Element documentElement = document.getDocumentElement();
		Element dependenciesElement = DOMTools.getElementByPath(documentElement, "dependencies");

		removeNonMatchingDependencies(dependenciesElement);

		writeTargetPom(document);
	}

	private void validate() {
		if (sourcePom == null) {
			throw new BuildException("Missing parameter 'sourcePom'");
		}

		if (!sourcePom.exists() || sourcePom.isDirectory()) {
			throw new BuildException("Parameter sourcePom must be an existing file. SourcePom: " + sourcePom.getAbsolutePath());
		}

		if (targetPom == null) {
			throw new BuildException("Missing parameter 'targetPom'");
		}
	}

	private void removeNonMatchingDependencies(Element dependenciesElement) {
		for (Node node = dependenciesElement.getFirstChild(); node != null;) {
			Node next = node.getNextSibling();

			if (isNonMatchingDependency(node)) {
				dependenciesElement.removeChild(node);
			}

			node = next;
		}
	}

	private boolean isNonMatchingDependency(Node currentNode) {
		if (currentNode.getNodeType() != Node.ELEMENT_NODE) {
			return false;
		}

		Element element = (Element) currentNode;
		if (!element.getTagName().equals("dependency")) {
			return false;
		}

		return !groups.contains(getGroup(element));
	}

	private String getGroup(Element dependencyElement) {
		for (Node node = dependencyElement.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE) {
				continue;
			}

			ProcessingInstruction pi = (ProcessingInstruction) node;
			if (!pi.getTarget().equals("group")) {
				continue;
			}

			return pi.getData();
		}

		return "default";
	}

	private Document loadSourcePom() {
		try {
			return XmlTools.loadXML(sourcePom);
		} catch (Exception e) {
			throw new BuildException("Error while loading source pom:" + sourcePom.getAbsolutePath(), e);
		}
	}

	private void writeTargetPom(Document document) {
		try {
			XmlTools.writeXml(document, targetPom, "UTF-8");

		} catch (Exception e) {
			throw new BuildException("Error while writing target pom:" + targetPom.getAbsolutePath(), e);
		}
	}

}
