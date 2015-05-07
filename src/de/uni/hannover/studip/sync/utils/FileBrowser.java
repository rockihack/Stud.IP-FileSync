package de.uni.hannover.studip.sync.utils;

import java.io.File;

import de.uni.hannover.studip.sync.models.Config;

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

		String filePath = file.getAbsolutePath();

		if (OS.isWindows()) {
			return runCommand(new String[] {"explorer", filePath});

		} else if (OS.isMacOS()) {
			return runCommand(new String[] {"open", filePath});

		} else if (OS.isLinux()) {
			// TODO: Escape whitespaces?
			return runCommand(new String[] {"xdg-open", filePath})			// All
					|| runCommand(new String[] {"kde-open", filePath})		// KDE
					|| runCommand(new String[] {"exo-open", filePath})		// Xfce
					|| runCommand(new String[] {"gvfs-open", filePath})		// GNOME
					|| runCommand(new String[] {"gnome-open", filePath})	// GNOME (deprecated)
					|| runCommand(new String[] {"pcmanfm", filePath});		// LXDE

		} else {
			return false;
		}
	}

	private static boolean runCommand(String[] cmd) {
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
		/* Replace whitespaces. */
		switch (Config.getInstance().getReplaceWhitespaces()) {
		case 1:
			file = file.replaceAll("[-\\s]+", "-");
			break;
		case 2:
			file = file.replaceAll("[_\\s]+", "_");
			break;
		default:
			break;
		}

		/* Replace separators. */
		file = file.replaceAll("[-\\/]+", "-");
		/* Remove other illegal chars. */
		return file.replaceAll("[<>:\"|?*]+", "");
	}
}
