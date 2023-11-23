package com.braintribe.build.ant.tasks;

import java.io.File;

import com.braintribe.logging.LoggerInitializer;

public interface HasLoggingConfiguration {
	default void initalizeLogging() {
	LoggerInitializer loggerInitializer = new LoggerInitializer();
	try {							
		File file = new File("logger.properties");
		//System.out.println( "logger properties expected at " + file.getAbsolutePath());
		if (file.exists()) {
			loggerInitializer.setLoggerConfigUrl( file.toURI().toURL());		
			loggerInitializer.afterPropertiesSet();
		}
	} catch (Exception e) {		
		e.printStackTrace();
	}
	}
}
