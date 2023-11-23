// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
// 
// This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
// 
// This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License along with this library; See http://www.gnu.org/licenses/.
// ============================================================================
package org.apache.tools.ant;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.tools.ant.listener.AnsiColorLogger;

/**
 * BT adaptation of {@link AnsiColorLogger}, which is configured based on an existing {@link DefaultLogger}.
 */
public class DrAnsiColorLogger extends DefaultLogger {

	public static final int MSG_BT_CUSTOM_THRESHOLD = 100;
	public static final int MSG_SPECIAL = MSG_BT_CUSTOM_THRESHOLD;
	public static final int MSG_ARTIFACT = MSG_BT_CUSTOM_THRESHOLD + 1;

	private static final int ATTR_NORMAL = 0;
	// private static final int ATTR_BRIGHT = 1;
	// private static final int ATTR_DIM = 2;
	// private static final int ATTR_UNDERLINE = 3;
	// private static final int ATTR_BLINK = 5;
	// private static final int ATTR_REVERSE = 7;
	// private static final int ATTR_HIDDEN = 8;

	// private static final int FG_BLACK = 30;
	private static final int FG_RED = 31;
	private static final int FG_GREEN = 32;
	private static final int FG_YELLOW = 33;
	// private static final int FG_BLUE = 34;
	// private static final int FG_MAGENTA = 35;
	private static final int FG_CYAN = 36;
	// private static final int FG_WHITE = 37;
	private static final int FG_DEFAULT = 39;

	private static final int FG_BRIGHT_WHITE = 97;

	// private static final int BG_BLACK = 40;
	// private static final int BG_RED = 41;
	// private static final int BG_GREEN = 42;
	// private static final int BG_YELLOW = 44;
	// private static final int BG_BLUE = 44;
	// private static final int BG_MAGENTA = 45;
	// private static final int BG_CYAN = 46;
	// private static final int BG_WHITE = 47;

	private static final String PREFIX = "\u001b[";
	private static final String SUFFIX = "m";
	private static final char SEPARATOR = ';';
	private static final String END_COLOR = PREFIX + SUFFIX;

	private static final String specialColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_BRIGHT_WHITE + SUFFIX;
	private static final String artifactColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_CYAN + SUFFIX;
	private static final String errColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_RED + SUFFIX;
	private static final String warnColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_YELLOW + SUFFIX;
	private static final String infoColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_DEFAULT + SUFFIX;
	private static final String verbColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_CYAN + SUFFIX;
	private static final String debugColor = PREFIX + ATTR_NORMAL + SEPARATOR + FG_GREEN + SUFFIX;

	private static volatile boolean ansiInfoPrinted = false;

	public DrAnsiColorLogger(DefaultLogger delegate) {
		/* When configuring the devrock jars externally (rather than copying them into the lib folder) they (and thus also this class) are loaded with
		 * a different ClassLoader than DefaultLogger. Hence accessing protected fields doesn't work, so we use reflection. */
		this.msgOutputLevel = getField(delegate, "msgOutputLevel");
		this.out = getField(delegate, "out");
		this.err = getField(delegate, "err");
		this.emacsMode = getField(delegate, "emacsMode");

		// to track time, will be a little off as the timer was already triggered in our delegate, but it's a minor difference
		super.buildStarted(null);

		printAnsiInstalled();
	}

	private <T> T getField(DefaultLogger delegate, String name) {
		try {
			Field declaredField = delegate.getClass().getDeclaredField(name);
			declaredField.setAccessible(true);
			return (T) declaredField.get(delegate);

		} catch (Exception e) {
			throw new RuntimeException("Error while accessing field '" + name + "' of DefaultLogger", e);
		}
	}

	private void printAnsiInstalled() {
		if (ansiInfoPrinted)
			return;

		ansiInfoPrinted = true;

		out.println();
		out.println(encode("ANSI console detected, color will follow: ", specialColor) + //
				encode("ERROR", Project.MSG_ERR) + " " + //
				encode("WARNING", Project.MSG_WARN) + " " + //
				encode("INFO", Project.MSG_INFO) + " " + //
				encode("VERBOSE", Project.MSG_VERBOSE) + " " + //
				encode("DEBUG", Project.MSG_DEBUG));
		out.println();
	}

	/** To be used internally */
	public void printMessage(String message, int priority) {
		printMessage(message, out, priority);
	}

	/**
	 * {@inheritDoc}.
	 * 
	 * @see DefaultLogger#printMessage
	 */
	@Override
	protected void printMessage(String message, PrintStream stream, int priority) {
		if (message != null && stream != null)
			stream.println(encode(message, priority));
	}

	public void printEncodedMessage(String encodedMessage) {
		if (encodedMessage != null)
			out.println(encodedMessage);
	}

	public static String encode(String message, int priority) {
		return encode(message, colorCode(priority));
	}

	private static String encode(String message, String colorCode) {
		return colorCode + message + END_COLOR;
	}

	private static String colorCode(int priority) {
		switch (priority) {
			case MSG_SPECIAL:
				return specialColor;
			case MSG_ARTIFACT:
				return artifactColor;
			case Project.MSG_ERR:
				return errColor;
			case Project.MSG_WARN:
				return warnColor;
			case Project.MSG_INFO:
				return infoColor;
			case Project.MSG_VERBOSE:
				return verbColor;
			case Project.MSG_DEBUG:
				// Fall through
			default:
				return debugColor;
		}
	}

	@Override
	protected String getBuildSuccessfulMessage() {
		return super.getBuildSuccessfulMessage() + " " + dateInfo();
	}

	@Override
	protected String getBuildFailedMessage() {
		return super.getBuildFailedMessage() + " " + dateInfo();
	}

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm yyyy-MM-dd");

	private String dateInfo() {
		return "(" + dateFormat.format(new Date()) + ")";
	}
}
