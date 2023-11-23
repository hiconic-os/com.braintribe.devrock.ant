package com.braintribe.build.ant.utils.console;

import java.util.Date;
import java.util.List;

import com.braintribe.console.ConsoleOutputs;
import com.braintribe.devrock.model.mc.cfg.origination.Origination;
import com.braintribe.devrock.model.mc.cfg.origination.RepositoryConfigurationResolving;
import com.braintribe.gm.model.reason.Reason;

public class ReasonConsoleOutput {
	private static final String lotsOfTabs ="\t\t\t\t\t\t\t\t\t\t\t";
	
	/**
	 * @param num - the number of tabs
	 * @return - a string of tabs, max length is defined by 'lotsOfTabs'
	 */
	private static String tabs( int num) {
		if (num > lotsOfTabs.length()) {
			num = lotsOfTabs.length() - 1;
		}
		return lotsOfTabs.substring(0, num);
	}
	/**
	 * pads a string with tabs 
	 * @param context
	 * @param string
	 * @return
	 */
	private static String pad( ConsoleOutputContext context,String string) {
		String pad = tabs( context.peekIndent());
		return pad + string;
	}
	/**
	 * pads a new line with tabs 
	 * @param context
	 * @param string
	 * @return
	 */
	private static String padL( ConsoleOutputContext context,String string) {
		return pad( context, string) + "\n";
	}
	
	public static void toConsoleContainer( ConsoleOutputContext context, Reason reason) {
		toConsoleContainer(context, reason, false);
	}
	/**
	 * recursively output reasons, child reasons are indented by a tab
	 * @param context - the {@link ConsoleOutputContext}
	 * @param reason - the {@link Reason} to output 
	 */
	public static void toConsoleContainer( ConsoleOutputContext context, Reason reason, boolean origination) {
		// 
		String type = reason.entityType().getTypeName();
		String text = reason.getText();		
		if (!origination) {
			context.consoleOutputContainer().append( padL( context, type + " : " + text));
		}
		else {
			context.consoleOutputContainer().append( padL( context, text));
		}
		// think of properties that could be output generically 		
		List<Reason> reasons = reason.getReasons();
		if (reasons.size() > 0) {
			context.pushIndent();
			for (Reason subReason : reasons) {
				toConsoleContainer(context, subReason, origination);
			}
			context.popIndent();
		}
	}
	
	/**
	 * writes the reasons formatted into the console
	 * @param reason
	 */
	public static void toConsole( Reason reason) {
		BasicConsoleOutputContext context = new BasicConsoleOutputContext();
		toConsoleContainer(context, reason);
		ConsoleOutputs.print( context.consoleOutputContainer());
	}
	
	/**
	 * produces a console container with the orgination data (top plus all subs)
	 * @param origination - the {@link Origination}
	 */
	public static void originationToConsole( Origination origination) {
		BasicConsoleOutputContext context = new BasicConsoleOutputContext();
		Date date = null;
		if (origination instanceof RepositoryConfigurationResolving) {
			date = ((RepositoryConfigurationResolving) origination).getTimestamp();
		}
		if (date != null) {
			context.consoleOutputContainer().append( padL(context, "repository configuration origination [" + date.toString() + "] :"));
		}
		else {
			context.consoleOutputContainer().append( padL(context, "repository configuration origination [undated] :"));
		}
		context.pushIndent();
		toConsoleContainer(context, origination, true);
		context.popIndent();		
		ConsoleOutputs.print( context.consoleOutputContainer());
	}
	
}
