package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Config data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigFile {

	// Path to studip sync root dir.
	// Default: null
	public String root_dir;
	
	// If true existing files will be renamed instead of overwritten.
	// Default: false
	public boolean rename_modified_files;
	
	public ConfigFile() {
	}
	
	public ConfigFile(String root_dir) {
		this.root_dir = root_dir;
	}
}
