package de.uni.hannover.studip.sync.datamodel;

/**
 * Config data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
public class SettingsFile {

	/**
	 * Path to studip sync root dir.
	 */
	public String rootDir;

	/**
	 * Folder structure template.
	 */
	public String folderStructure;

	/**
	 * If true existing files will be overwritten,
	 * otherwise a version number is appended.
	 */
	public boolean overwriteFiles = true;

	/**
	 * If true documents from all visible semesters will be downloaded,
	 * otherwise only from the current semester.
	 */
	public boolean downloadAllSemesters;

	/**
	 * 0: Do not replace whitespaces
	 * 1: Replace with "-"
	 * 2: Replace with "_"
	 */
	public int replaceWhitespaces;

	/**
	 * Set true to enable freely customizable folder and file names
	 */
	public boolean folderConf = false;

}
