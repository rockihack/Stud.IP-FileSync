package de.uni.hannover.studip.sync.utils;

import java.util.Locale;

/**
 * Operating system helper class.
 * 
 * @author Lennart Glauer
 *
 */
public enum OS {

	UNKOWN,
	WINDOWS,
	MACOS,
	LINUX,
	SOLARIS;

	private static final OS CURRENT_OS = detectOS();

	public static OS get() {
		return CURRENT_OS;
	}

	public static boolean isWindows() {
		return CURRENT_OS == WINDOWS;
	}

	public static boolean isMacOS() {
		return CURRENT_OS == MACOS;
	}

	public static boolean isLinux() {
		return CURRENT_OS == LINUX || CURRENT_OS == SOLARIS;
	}

	/**
	 * Detect current operating system.
	 * 
	 * @return
	 */
	private static OS detectOS() {
		final String os = System.getProperty("os.name").toLowerCase(Locale.GERMANY);

		if (os.contains("win")) {
			return WINDOWS;

		} else if (os.contains("mac")) {
			return MACOS;

		} else if (os.contains("linux") || os.contains("unix")) {
			return LINUX;

		} else if (os.contains("solaris") || os.contains("sunos")) {
			return SOLARIS;

		} else {
			return UNKOWN;
		}
	}

}
