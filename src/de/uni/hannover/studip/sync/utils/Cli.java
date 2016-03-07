package de.uni.hannover.studip.sync.utils;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.TreeSync;

/**
 * Cli helper.
 * 
 * @author Lennart Glauer
 *
 */
public final class Cli {

	private static final Config CONFIG = Config.getInstance();
	private static final OAuth OAUTH = OAuth.getInstance();

	private Cli() {
		// Utility class.
	}

	public static void handleArgs(final String[] args) {
		boolean sync = false;

		for (final String arg : args) {
			switch (arg) {
			case "-s":
			case "--sync":
				sync = true;
				break;
			default:
				System.out.println("Invalid argument.");
				System.exit(1);
				break;
			}
		}

		if (sync) {
			System.exit(handleSync());
		}
	}

	private static int handleSync() {
		if (!OAUTH.restoreAccessToken()) {
			OAUTH.removeAccessToken();
			System.out.println("Invalid oauth access token. Abort.");
			return 1;
		}

		final String rootDir = CONFIG.getRootDirectory();
		if (rootDir == null || rootDir.isEmpty()) {
			System.out.println("Invalid root directory. Abort.");
			return 2;
		}

		if (!Main.TREE_LOCK.tryLock()) {
			System.out.println("Failed to lock the tree. Abort.");
			return 3;
		}

		try (final TreeSync tree = new TreeSync(Paths.get(rootDir))) {
			final Path treeFile = Config.openTreeFile();

			/* Update documents. */
			try {
				System.out.println("Updating...");
				tree.update(treeFile);

			} catch (NoSuchFileException | JsonParseException | JsonMappingException e) {
				/* Invalid tree file. */
				System.out.println("Building new tree...");
				tree.build(treeFile);
			}

			/* Download documents. */
			System.out.println("Downloading...");
			final int newDocuments = tree.sync(treeFile, CONFIG.isDownloadAllSemesters());
			if (newDocuments > 0) {
				System.out.println("New documents: " + newDocuments);
			}

			System.out.println("Done.");
			return 0;

		} catch (IOException e) {
			System.out.println(e.getMessage());
			return 4;

		} finally {
			Main.TREE_LOCK.unlock();
		}
	}

}
