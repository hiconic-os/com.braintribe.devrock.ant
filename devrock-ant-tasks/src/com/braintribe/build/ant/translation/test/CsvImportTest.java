// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.test;

import com.braintribe.build.ant.translation.model.Model;
import com.braintribe.build.ant.translation.model.SynchronizationParams;

public class CsvImportTest {

	public static void main(String[] args) {
		if (args.length != 5) {
			System.out.println("Usage: Test sourcePath cvsPath groupId artifactId version\n");
			System.out.println("sourcePath: path of the folder containing the source files to be scanned.");
			System.out.println("cvsPath: path of the output .csv file (if existing, it will be rewrited).");
			System.out.println("groupId: the groupId of the source being analysed.");
			System.out.println("artifactId: the artifactId of the source being analysed.");
			System.out.println("version: the version of the source being analysed.");
			System.exit(0);
		}
		
		String sourcePath = args[0];
		String csvPath = args[1];
		String groupId = args[2];
		String artifactId = args[3];
		String version = args[4];
		
		Model csvModel = new Model();
		Model sourceModel = new Model();
		csvModel.updateModelFromCsv(csvPath);
		sourceModel.updateModelFromSourcePath(sourcePath, groupId, artifactId, version);
		sourceModel.syncFrom(csvModel, new SynchronizationParams());
		
		sourceModel.updateSourceFromModel("/Users/micheldocouto/Work/Test", groupId, artifactId, version);
	}
}
