package de.uni.hannover.studip.sync.utils;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
	 * @param file File/Folder to open
	 * @return True if successful
	 * @throws IOException 
	 */
	public static boolean open(final Path file) throws IOException {
		if (!Files.exists(file)) {
			throw new IOException("File not found!");
		}

		if (OS.isWindows()) {
			try {
				// Windows long path names workaround.
				if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
					throw new IOException("Desktop not supported!");
				}

				Desktop.getDesktop().open(file.toFile());
				return true;

			} catch (IOException e) {
				return runCommand("explorer", file.toAbsolutePath().toString());
			}

		} else if (OS.isMacOS()) {
			return runCommand("open", file.toAbsolutePath().toString());

		} else if (OS.isLinux()) {
			final String filePath = file.toAbsolutePath().toString();

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
	 * @param cmd Command to execute
	 * @return True if successful
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
	 * @param fileName Original filename
	 * @param replaceWhitespaces Replace option
	 * @return New filename
	 */
	public static String removeIllegalCharacters(String fileName, final int replaceWhitespaces) {
		/* Remove leading and trailing whitespaces. */
		fileName = fileName.trim();

		/* Replace whitespaces. */
		switch (replaceWhitespaces) {
		case 1:
			fileName = fileName.replaceAll("[-\\s]+", "-");
			break;
		case 2:
			fileName = fileName.replaceAll("[_\\s]+", "_");
			break;
		default:
			break;
		}

		/* Replace separators. */
		fileName = fileName.replaceAll("[-\\/]+", "-");
		/* Remove other illegal chars. */
		return fileName.replaceAll("[<>:\"|?*]+", "");
	}

	/**
	 * Remove/replace illegal chars from path/file name.
	 * 
	 * @param fileName Original filename
	 * @return New filename
	 */
	public static String removeIllegalCharacters(final String fileName) {
		return removeIllegalCharacters(fileName, Config.getInstance().getReplaceWhitespaces());
	}

	/**
	 * Appends the suffix to the filename (before file extension).
	 * 
	 * @param fileName Original filename
	 * @param suffix Suffix to append
	 * @return New filename
	 */
	public static String appendFilename(final String fileName, final String suffix) {
		final int ext = fileName.lastIndexOf('.');

		return ext == -1
				? fileName + suffix
				: fileName.substring(0, ext) + suffix + fileName.substring(ext);
	}
}
