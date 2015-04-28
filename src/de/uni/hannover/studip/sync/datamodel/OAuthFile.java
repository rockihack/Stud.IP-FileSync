package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OAuth data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthFile {

	// Name and id of the currently logged in user.
	public String first_name = null;
	public String last_name = null;

	public String user_name = null;
	public String user_id = null;

	// OAuth access token and secret.
	public String token = null;
	public String secret = null;
	
	public OAuthFile() {
	}
	
	public OAuthFile(String first_name, String last_name, String user_name, String user_id, String token, String secret) {
		this.first_name = first_name;
		this.last_name = last_name;
		this.user_name = user_name;
		this.user_id = user_id;
		this.token = token;
		this.secret = secret;
	}
	
}
