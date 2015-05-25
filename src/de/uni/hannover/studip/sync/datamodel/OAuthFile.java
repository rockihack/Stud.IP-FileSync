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
	public String firstName;
	public String lastName;
	public String userName;

	/**
	 * Logged in user id.
	 */
	public String userId;

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
		this.firstName = first_name;
		this.lastName = last_name;
		this.userName = user_name;
		this.userId = user_id;
		this.token = token;
		this.secret = secret;
	}
	
}
