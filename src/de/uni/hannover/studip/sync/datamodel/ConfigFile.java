package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Config data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigFile {

	public String root_dir;
	
	public boolean rename_modified_files;
	
	public ConfigFile() {
	}
	
	public ConfigFile(String root_dir) {
		this.root_dir = root_dir;
	}
}
