package de.uni.hannover.studip.sync.utils;

import java.util.Locale;

/**
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

	private static OS currentOS = detectOS();

	public static boolean isWindows() {
		return currentOS == WINDOWS;
	}

	public static boolean isMacOS() {
		return currentOS == MACOS;
	}

	public static boolean isLinux() {
		return currentOS == LINUX || currentOS == SOLARIS;
	}

	private static OS detectOS() {
		String os = System.getProperty("os.name").toLowerCase(Locale.GERMANY);

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
