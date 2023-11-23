package com.braintribe.build.ant.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.braintribe.build.ant.mc.Bridges;
import com.braintribe.build.ant.mc.McBridge;

/**
 * real simple task to retrieve the current 'processing data insight folder'.
 * path is returned in a property, default is 'processing-data-insight-folder'
 * 
 * @author pit
 *
 */
public class DetermineProcessingDataInsightFolder extends Task{
	private String property = "processing-data-insight-folder";
	private boolean ensure;
	
	/**
	 * @param property - overrides the standard property where the path is returned
	 */
	public void setPropertyName(String property) {
		this.property = property;
	}
	
	/**
	 * @param ensure - true if folder's (sub)-directories should be created if not existing
	 */
	public void setEnsureFolder(boolean ensure) {
		this.ensure = ensure;
	}

	@Override
	public void execute() throws BuildException {
		McBridge mcBridge = Bridges.getInstance(getProject());
		File problemAnalysisFolder = mcBridge.getProcessingDataInsightFolder();
		if (!problemAnalysisFolder.exists() && ensure) {
			problemAnalysisFolder.mkdirs();
		}
		getProject().setProperty(property, problemAnalysisFolder.getAbsolutePath());			
	}
	

	

}
