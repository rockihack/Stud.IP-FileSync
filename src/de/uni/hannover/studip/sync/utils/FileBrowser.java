package de.uni.hannover.studip.sync.utils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import de.uni.hannover.studip.sync.models.Config;

/**
 * File browser utility class.
 * 
 * @author Lennart Glauer
 * 
 * @notice java.awt.Desktop does not work on linux.
 *
 */
public final class FileBrowser {

	private FileBrowser() {
		// Utility class.
	}

	/**
	 * Opens a file or directory in the users default browser.
	 * 
	 * @param file
	 * @return
	 */
	public static boolean open(final File file) {
		if (!file.exists()) {
			return false;
		}

		if (OS.isWindows()) {
			try {
				// Windows long path names workaround.
				if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
					throw new IOException("Desktop not supported!");
				}

				Desktop.getDesktop().open(file);
				return true;

			} catch (IOException e) {
				return runCommand("explorer", file.getAbsolutePath());
			}

		} else if (OS.isMacOS()) {
			return runCommand("open", file.getAbsolutePath());

		} else if (OS.isLinux()) {
			final String filePath = file.getAbsolutePath();

			return runCommand("xdg-open", filePath)			// All
					|| runCommand("kde-open", filePath)		// KDE
					|| runCommand("exo-open", filePath)		// Xfce
					|| runCommand("gvfs-open", filePath)	// GNOME
					|| runCommand("gnome-open", filePath)	// GNOME (deprecated)
					|| runCommand("pcmanfm", filePath);		// LXDE

		} else {
			return false;
		}
	}

	/**
	 * Run a shell command.
	 * 
	 * @param cmd
	 * @return
	 */
	private static boolean runCommand(final String... cmd) {
		try {
			final Process process = Runtime.getRuntime().exec(cmd);
			return process.isAlive();

		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Remove/replace illegal chars from path/file name.
	 * 
	 * @param file
	 * @return
	 */
	public static String removeIllegalCharacters(String file, final int replaceWhitespaces) {
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
	public static String removeIllegalCharacters(final String file) {
		return removeIllegalCharacters(file, Config.getInstance().getReplaceWhitespaces());
	}

	/**
	 * Appends the suffix to the filename (before file extension).
	 * 
	 * @param filename
	 * @param suffix
	 * @return
	 */
	public static String appendFilename(final String filename, final String suffix) {
		int ext = filename.lastIndexOf('.');
		if (ext == -1) {
			ext = filename.length();
		}

		return filename.substring(0, ext) + suffix + filename.substring(ext);
	}
}
