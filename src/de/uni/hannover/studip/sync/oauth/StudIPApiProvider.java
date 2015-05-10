package de.uni.hannover.studip.sync.oauth;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/**
 * Stud.IP Api Provider for Scribe OAuth 1.0a.
 * 
 * @author Lennart Glauer
 */
public final class StudIPApiProvider extends DefaultApi10a
{
	/**
	 * Studip rest api plugin base url.
	 */
	public static final String BASE_URL = "https://elearning.uni-hannover.de/plugins.php/restipplugin";

	/**
	 * OAuth service callback address.
	 */
	public static final String API_CALLBACK = "https://elearning.uni-hannover.de/index.php";

	/**
	 * Logout address.
	 */
	public static final String LOGOUT = "https://elearning.uni-hannover.de/logout.php";

	/**
	 * OAuth consumer key and secret.
	 */
	public static final String API_KEY = "";
	public static final String API_SECRET = "";

	@Override
	public String getAccessTokenEndpoint()
	{
		return BASE_URL + "/oauth/access_token";
	}

	@Override
	public String getRequestTokenEndpoint()
	{
		return BASE_URL + "/oauth/request_token";
	}

	@Override
	public String getAuthorizationUrl(final Token requestToken)
	{
		return BASE_URL + "/oauth/authorize?oauth_token=" + requestToken.getToken();
	}
}
