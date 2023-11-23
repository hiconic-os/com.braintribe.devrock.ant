package com.braintribe.build.ant.utils.console;

import java.util.Stack;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.console.ConsoleOutputs;
import com.braintribe.console.output.ConfigurableConsoleOutputContainer;

/**
 * @author pit
 *
 */
public class BasicConsoleOutputContext implements ConsoleOutputContext {
	private ConfigurableConsoleOutputContainer consoleOutputContainer;	
	private Stack<Integer> stack = new Stack<>();
	
	public BasicConsoleOutputContext() {
		consoleOutputContainer = ConsoleOutputs.configurableSequence();
	}

	@Override
	public ConfigurableConsoleOutputContainer consoleOutputContainer() {		
		return consoleOutputContainer;
	}
	@Configurable @Required
	public void setConsoleOutputContainer(ConfigurableConsoleOutputContainer consoleOutputContainer) {
		this.consoleOutputContainer = consoleOutputContainer;
	}
	
	@Override
	public int peekIndent() {
		if (stack.isEmpty())
			return 0;
		return stack.peek();
	}
	@Override
	public void pushIndent() {
		if (!stack.isEmpty()) {
			stack.push( stack.peek() + 1);
		}
		else {
			stack.push( 1);
		}		
	}
	
	@Override
	public void popIndent() {
		if (!stack.isEmpty()) {
			stack.pop();
		}		
	}
		
}
