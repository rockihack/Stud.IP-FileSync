package de.uni.hannover.studip.sync.utils;

import java.io.File;

/**
 * 
 * @author Lennart Glauer
 * 
 * @notice java.awt.Desktop does not work on linux.
 *
 */
public class FileBrowser {

	public static boolean open(File file) {
		if (!file.exists()) {
			return false;
		}

		if (OS.isWindows()) {
			return runCommand(String.format("explorer %s", file.getAbsolutePath()));

		} else if (OS.isMacOS()) {
			return runCommand(String.format("open %s", file.getAbsolutePath()));

		} else if (OS.isLinux()) {
			return runCommand(String.format("xdg-open %s", file.getAbsolutePath()))			// All
					|| runCommand(String.format("kde-open %s", file.getAbsolutePath()))		// KDE
					|| runCommand(String.format("exo-open %s", file.getAbsolutePath()))		// Xfce
					|| runCommand(String.format("gvfs-open %s", file.getAbsolutePath()))	// GNOME
					|| runCommand(String.format("gnome-open %s", file.getAbsolutePath()))	// GNOME (deprecated)
					|| runCommand(String.format("pcmanfm %s", file.getAbsolutePath()));		// LXDE

		} else {
			return false;
		}
	}

	private static boolean runCommand(String cmd) {
		try {
			return Runtime.getRuntime().exec(cmd) != null;

		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Remove/replace illegal chars from path/file name.
	 * 
	 * @param file
	 * @return
	 */
	public static String removeIllegalCharacters(String file) {
		/* Replace separators. */
		file = file.replaceAll("[\\/]+", "-");
		/* Remove other illegal chars. */
		return file.replaceAll("[<>:\"|?*]+", "");
	}
}
