package com.braintribe.build.ant.tasks;

import java.io.Console;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import org.apache.tools.ant.Project;

import com.braintribe.console.ConsoleConfiguration;
import com.braintribe.console.PrintStreamConsole;
import com.braintribe.console.VoidConsole;

public class ColorSupport {
	
	private Boolean shouldUseColors;
	
	private final Project project;
	
	public ColorSupport(Project project) {
		super();
		this.project = project;
	}

	private boolean shouldUseColors() {
		if (shouldUseColors == null)
			shouldUseColors = findIfShouldUseColors();
		return shouldUseColors;
	}

	private boolean findIfShouldUseColors() {
		String color = project.getProperty("colors");
		if (color == null)
			color = System.getenv("BT__ANT_COLORS");
		
		if (color == null)
			return true;
		
		return color.toLowerCase().equals("true");
	}
	
	public void installConsole() {
		if (com.braintribe.console.Console.get() != VoidConsole.INSTANCE)
			return;

		try {
//			Project project = getProject();
//			DefaultLogger defaultLogger = (DefaultLogger) project.getBuildListeners().stream() //
//					.filter(l -> l instanceof DefaultLogger) //
//					.findFirst() //
//					.orElse(null);
//			
//			Field field = DefaultLogger.class.getDeclaredField("out");
//			field.setAccessible(true);
//			PrintStream out = (PrintStream)field.get(defaultLogger);

			Charset cs = guessConsoleEncoding();
			PrintStream ps = new PrintStream(System.out, true, cs.name());
			ConsoleConfiguration.install(new PrintStreamConsole(ps, shouldUseColors(), false));
			//ConsoleConfiguration.install(new AppendableConsole(new LogAppendable(ps), true));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    public static Charset guessConsoleEncoding() {
        // We cannot use Console class directly, because we also need the access to the raw byte stream,
        // e.g. for pushing in a raw output from a forked VM invocation. Therefore, we are left with
        // reflectively poking out the Charset from Console, and use it for our own private output streams.

    	// leniently access java 17's Console.charset() method by using reflection access;
    	try {
			Method method = Console.class.getMethod("charset");
			Charset cs = (Charset) method.invoke(System.console());
			return cs;
		} catch (Exception e) {
			return Charset.defaultCharset();
		}
    }
}
