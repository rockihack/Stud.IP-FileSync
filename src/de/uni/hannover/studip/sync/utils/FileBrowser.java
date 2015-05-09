package de.uni.hannover.studip.sync.utils;

import java.awt.Desktop;
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

		if (OS.isWindows()) {
			try {
				// Windows long path names workaround.
				if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
					throw new UnsupportedOperationException("Desktop not supported!");
				}

				Desktop.getDesktop().open(file);
				return true;

			} catch (Exception e) {
				return runCommand(new String[] {"explorer", file.getAbsolutePath()});
			}

		} else if (OS.isMacOS()) {
			return runCommand(new String[] {"open", file.getAbsolutePath()});

		} else if (OS.isLinux()) {
			// TODO: Escape whitespaces?
			String filePath = file.getAbsolutePath();

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
	public static String removeIllegalCharacters(String file, int replaceWhitespaces) {
		/* Replace whitespaces. */
		switch (replaceWhitespaces) {
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

	/**
	 * Remove/replace illegal chars from path/file name.
	 * 
	 * @param file
	 * @return
	 */
	public static String removeIllegalCharacters(String file) {
		return removeIllegalCharacters(file, Config.getInstance().getReplaceWhitespaces());
	}
}
