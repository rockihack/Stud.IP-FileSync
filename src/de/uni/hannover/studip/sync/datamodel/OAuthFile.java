package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OAuth data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthFile {

	public String token;
	public String secret;
	
	public OAuthFile() {
	}
	
	public OAuthFile(String token, String secret) {
		this.token = token;
		this.secret = secret;
	}
	
}
