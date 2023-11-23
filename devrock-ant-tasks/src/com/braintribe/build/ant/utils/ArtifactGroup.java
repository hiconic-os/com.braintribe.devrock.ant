// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================

package com.braintribe.build.ant.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArtifactGroup {
	private File groupDirectory;
	private File parentPom;
	private List<File> memberDirectories = new ArrayList<>();
	
	public File getGroupDirectory() {
		return groupDirectory;
	}
	public void setGroupDirectory(File groupDirectory) {
		this.groupDirectory = groupDirectory;
	}
	public File getParentPom() {
		return parentPom;
	}
	public void setParentPom(File parentPom) {
		this.parentPom = parentPom;
	}
	public void setMemberDirectories(File[] subs) {
		memberDirectories.addAll( Arrays.asList( subs));		
	}
	public List<File> getMemberDirectories() {
		return memberDirectories;
	}
	
	public void setMemberDirectories(List<File> memberDirectories) {
		this.memberDirectories = memberDirectories;
	}
	
	
}
