// ============================================================================
// BRAINTRIBE TECHNOLOGY GMBH - www.braintribe.com
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2018 - All Rights Reserved
// It is strictly forbidden to copy, modify, distribute or use this code without written permission
// To this file the Braintribe License Agreement applies.
// ============================================================================


package com.braintribe.build.ant.translation.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Translation {
	
	private String key;
	
	private Map<String, String> translations = new LinkedHashMap<String, String>();

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Map<String, String> getTranslations() {
		return translations;
	}

	public void setTranslations(Map<String, String> translations) {
		this.translations = translations;
	}

	public void syncFrom(Translation otherTranslation, SynchronizationParams params) {
		for (Map.Entry<String, String> entry: otherTranslation.translations.entrySet()) {
			String locale = entry.getKey();
			String value = entry.getValue();
			
			String existingValue = translations.get(locale); 
			if (existingValue != null) {
				switch (params.getPreferredSource()) {
				case from:
					translations.put(locale, value);
					break;
				case to:
					// do nothing because "to" is already there 
					break;
				}
			}
			else {
				System.out.println(value);
				translations.put(locale, value);
			}
		}
	}

}
