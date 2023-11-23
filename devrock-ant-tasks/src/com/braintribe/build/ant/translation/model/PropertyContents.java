// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.model;

import java.util.Map;

public class PropertyContents {
	
	private String languageName;
	
	private Map<String, String> translationsMap;

	public String getLanguageName() {
		return languageName;
	}

	public void setLanguageName(String languageName) {
		this.languageName = languageName;
	}

	public Map<String, String> getTranslationsMap() {
		return translationsMap;
	}

	public void setTranslationsMap(Map<String, String> translationsMap) {
		this.translationsMap = translationsMap;
	}

}
