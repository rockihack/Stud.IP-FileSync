package de.uni.hannover.studip.sync.oauth;

import org.scribe.builder.api.DefaultApi10a;
import org.scribe.model.Token;

/**
 * Stud.IP Api Provider for Scribe OAuth 1.0a.
 * 
 * @author Lennart Glauer
 */
public class StudIPApiProvider extends DefaultApi10a
{
	/* TODO: Allow other universities. */
	private static final String BASE_URL = "elearning.uni-hannover.de/plugins.php/restipplugin";
	
	private static final String AUTHORIZE_URL = BASE_URL + "/oauth/authorize?oauth_token=%s";
	private static final String REQUEST_TOKEN_RESOURCE = BASE_URL + "/oauth/request_token";
	private static final String ACCESS_TOKEN_RESOURCE = BASE_URL + "/oauth/access_token";

	@Override
	public String getAccessTokenEndpoint()
	{
		return "https://" + ACCESS_TOKEN_RESOURCE;
	}

	@Override
	public String getRequestTokenEndpoint()
	{
		return "https://" + REQUEST_TOKEN_RESOURCE;
	}

	@Override
	public String getAuthorizationUrl(Token requestToken)
	{
		return "https://" + String.format(AUTHORIZE_URL, requestToken.getToken());
	}
}
