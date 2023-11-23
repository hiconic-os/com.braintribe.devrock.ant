// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.process;


public class ProcessResults {
	private String errorText;
	private String normalText;
	private int retVal;
	
	public ProcessResults(int retVal, String normalText, String errorText) {
		super();
		this.errorText = errorText;
		this.normalText = normalText;
		this.retVal = retVal;
	}
	
	public String getErrorText() {
		return errorText;
	}
	
	public String getNormalText() {
		return normalText;
	}
	
	public int getRetVal() {
		return retVal;
	}
	
	
}
