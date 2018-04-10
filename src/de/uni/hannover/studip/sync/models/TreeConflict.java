package de.uni.hannover.studip.sync.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.elanev.studip.android.app.backend.datamodel.Document;
import de.elanev.studip.android.app.backend.datamodel.DocumentFolder;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileBrowser;

public final class TreeConflict {

	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private TreeConflict() {
		// Utility class.
	}

	/**
	 * Resolve folder name conflicts.
	 * 
	 * @notice The folder name might be modified.
	 * @param fileIndex Parent folder's filename index.
	 * @param fileIndexMap Maps folder name to filename index.
	 * @param folder Current folder.
	 */
	public static Set<String> resolveFolderNameConflict(final Set<String> fileIndex, final Map<String, Set<String>> fileIndexMap, final DocumentFolder folder) {
		if (StudIPApiProvider.DEFAULT_FOLDER.equals(folder.name.trim())) {
			/* Merge default folder with parent. */
			return fileIndex;
		}

		/* Use lowercase name because Windows and MacOS filesystems are case insensitive. */
		String folderName = FileBrowser.removeIllegalCharacters(folder.name).toLowerCase(Locale.GERMANY);
		if (!fileIndexMap.containsKey(folderName)) {
			/* Folder does not exist yet. */
			synchronized (fileIndex) {
				if (fileIndex.contains(folderName)) {
					/* Resolve file name conflict. */
					if (LOG.isLoggable(Level.WARNING)) {
						LOG.warning("Folder/file name conflict: " + folderName);
					}

					folder.name = FileBrowser.appendFilename(folder.name, "_" + folder.id);
					folderName = FileBrowser.removeIllegalCharacters(folder.name).toLowerCase(Locale.GERMANY);
				}

				/* Mark name as used. */
				fileIndex.add(folderName);
			}

			/* Create folder filename index. */
			fileIndexMap.put(folderName, new HashSet<String>());

		} else {
			/* Merge folders. */
			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Merge folders: " + folderName);
			}
		}

		return fileIndexMap.get(folderName);
	}

	/**
	 * Resolve file name conflicts.
	 * 
	 * @notice The document filename might be modified.
	 * @param fileIndex Folder filename index.
	 * @param document Folder document.
	 */
	public static void resolveFileNameConflict(final Set<String> fileIndex, final Document document) {
		/* Use lowercase name because Windows and MacOS filesystems are case insensitive. */
		String fileName = FileBrowser.removeIllegalCharacters(document.name).toLowerCase(Locale.GERMANY);

		synchronized (fileIndex) {
			if (fileIndex.contains(fileName)) {
				/* File name already exists. */
				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("File name conflict: " + fileName);
				}

				final Date chDate = new Date(document.chdate * 1000L);
				final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY);

				/* 1. Append change date.*/
				document.name = FileBrowser.appendFilename(document.name, "_" + format.format(chDate));
				fileName = FileBrowser.removeIllegalCharacters(document.name).toLowerCase(Locale.GERMANY);

				if (fileIndex.contains(fileName)) {
					/* 2. Append Stud.IP document id. */
					document.name = FileBrowser.appendFilename(document.name, "_" + document.id);
					fileName = FileBrowser.removeIllegalCharacters(document.name).toLowerCase(Locale.GERMANY);
				}

				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("Resolved to: " + fileName);
				}
			}

			/* Mark name as used. */
			fileIndex.add(fileName);
		}
	}
}
