package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Config data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettingsFile {

	// Path to studip sync root dir.
	public String root_dir = null;

	// If true, existing files will be overwritten,
	// otherwise a version number is appended.
	public boolean overwrite_files = true;

	// If true, documents from all visible semesters
	// will be downloaded.
	public boolean download_all_semesters = false;

	// 0: Do not replace whitespaces
	// 1: Replace with -
	// 2: Replace with _
	public int replaceWhitespaces = 0;

	public SettingsFile() {
	}
	
	public SettingsFile(String root_dir) {
		this.root_dir = root_dir;
	}
}
