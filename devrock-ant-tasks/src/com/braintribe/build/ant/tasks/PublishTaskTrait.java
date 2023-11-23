package com.braintribe.build.ant.tasks;

import java.io.File;

import org.apache.tools.ant.Task;

import com.braintribe.build.ant.utils.FileUtil;
import com.braintribe.model.artifact.consumable.Artifact;

public interface PublishTaskTrait {
	default void addPartsFromDirectory(Task task, File directory, Artifact transferArtifact) {
		// enumerate all files
		File[] files = directory.listFiles();
		
		for (File file : files) {
			String name = file.getName();
			
			if (
					(name.startsWith("maven-metadata-") && name.endsWith(".xml")) || 
					name.startsWith("part-availability-") ||
					name.endsWith(".solution") ||
					name.endsWith(".outdated")
					)
				continue;
			
			FileUtil.addPartFromCanonizedOrOtherFile(task, transferArtifact, file, true);
		}
	}
		
}
