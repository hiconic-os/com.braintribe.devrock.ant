package com.braintribe.build.ant.utils;

import com.braintribe.devrock.mc.api.commons.PartIdentifications;
import com.braintribe.model.artifact.essential.PartIdentification;

public interface PartUtils {
	static PartIdentification fromPartType(String partType)  {
		
		switch( partType.toUpperCase()) {
			case "JAR":
				return PartIdentifications.jar;
			case "POM":
				return PartIdentifications.pom;
			case "JAVADOC":
				return PartIdentifications.javadoc_jar;
			case "SOURCES":
				return PartIdentifications.sources_jar;
			case "EXCLUSIONS":
				return PartIdentification.create("exclusions");
			case "MD5":
				return PartIdentification.create("md5");
			case "ASC":
				return PartIdentification.create("asc");
			case "SHA1":
				return PartIdentification.create("sha1");
			case "PROJECT":
				return PartIdentification.create("project");
			case "ANT":
				return PartIdentification.create("ant");
			case "META":
				return PartIdentification.create("meta:xml");
			case "GLOBAL_META":
				return PartIdentification.create("globalmeta", "xml");
			case "MODEL":
				return PartIdentification.create("model", "xml");
			case "_UNKNOWN_":
			default:
				return PartIdentification.parse(partType);
		}
	}

}
