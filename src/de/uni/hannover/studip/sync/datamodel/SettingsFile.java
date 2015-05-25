package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Config data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingsFile {

	/**
	 * Path to studip sync root dir.
	 */
	public String rootDir;

	/**
	 * If true existing files will be overwritten,
	 * otherwise a version number is appended.
	 */
	public boolean overwriteFiles = true;

	/**
	 * If true documents from all visible semesters will be downloaded,
	 * otherwise only from the current semester.
	 */
	public boolean downloadAllSemesters = false;

	/**
	 * 0: Do not replace whitespaces
	 * 1: Replace with "-"
	 * 2: Replace with "_"
	 */
	public int replaceWhitespaces = 0;

	public SettingsFile() {
		// Needed for json object binding.
	}

	public SettingsFile(final String rootDir) {
		this.rootDir = rootDir;
	}
}
