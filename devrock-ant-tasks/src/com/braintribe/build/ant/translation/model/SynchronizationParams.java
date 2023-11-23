// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.model;

public class SynchronizationParams {
	public enum PreferredSource {
		from, to
	}
	
	private PreferredSource preferredSource = PreferredSource.to;
	
	public void setPreferredSource(PreferredSource preferredSource) {
		this.preferredSource = preferredSource;
	}
	
	public PreferredSource getPreferredSource() {
		return preferredSource;
	}
}
