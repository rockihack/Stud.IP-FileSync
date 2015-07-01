package de.uni.hannover.studip.sync.datamodel;

/**
 * OAuth data model used for json object binding.
 * 
 * @author Lennart Glauer
 */
public class OAuthFile {

	/**
	 * Logged in user firstname.
	 */
	public String firstName;

	/**
	 * Logged in user lastname.
	 */
	public String lastName;

	/**
	 * Logged in user name.
	 */
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

}
