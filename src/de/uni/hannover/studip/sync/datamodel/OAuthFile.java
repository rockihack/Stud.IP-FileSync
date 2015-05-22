package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * OAuth data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuthFile {

	/**
	 * Logged in user name.
	 */
	public String first_name;
	public String last_name;
	public String user_name;

	/**
	 * Logged in user id.
	 */
	public String user_id;

	/**
	 * OAuth access token.
	 */
	public String token;

	/**
	 * OAuth access secret.
	 */
	public String secret;

	public OAuthFile() {
		// Needed for json object binding.
	}

	public OAuthFile(final String first_name, final String last_name, final String user_name,
			final String user_id, final String token, final String secret) {
		this.first_name = first_name;
		this.last_name = last_name;
		this.user_name = user_name;
		this.user_id = user_id;
		this.token = token;
		this.secret = secret;
	}
	
}
