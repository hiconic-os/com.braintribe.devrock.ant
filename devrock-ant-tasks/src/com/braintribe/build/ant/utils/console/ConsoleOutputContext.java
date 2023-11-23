package com.braintribe.build.ant.utils.console;

import com.braintribe.console.output.ConfigurableConsoleOutputContainer;

/**
 * @author pit
 *
 */
public interface ConsoleOutputContext {
	ConfigurableConsoleOutputContainer consoleOutputContainer();
	int peekIndent();
	void pushIndent();
	void popIndent();	
}
