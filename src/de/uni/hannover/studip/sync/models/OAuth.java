package de.uni.hannover.studip.sync.models;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;

/**
 * OAuth model.
 * 
 * @author Lennart Glauer
 */
public class OAuth {
	
	/**
	 * OAuth consumer key and secret.
	 */
	private static final String API_KEY = "<api-key>";
	private static final String API_SECRET = "<api-secret>";
	
	/**
	 * OAuth service callback address.
	 */
	private static final String API_CALLBACK = "https://elearning.uni-hannover.de/index.php";
	
	/**
	 * Singleton instance.
	 */
	private static OAuth singletonInstance = null;
	
	/**
	 * Singleton instance getter.
	 * 
	 * @return OAuth instance
	 */
	public static OAuth getInstance() {
		if (singletonInstance == null) {
			singletonInstance = new OAuth();
		}
		
		return singletonInstance;
	}
	
	/**
	 * Service object.
	 */
	private OAuthService service;
	
	/**
	 * Request token.
	 */
	private Token requestToken;
	
	/**
	 * Auth url.
	 */
	private String authUrl;
	
	/**
	 * Access token.
	 */
	private Token accessToken;

	/**
	 * Step 1: Create the OAuthService object.
	 */
	private OAuth() {
		service = new ServiceBuilder()
			.provider(StudIPApiProvider.class)
			.apiKey(API_KEY)
			.apiSecret(API_SECRET)
			.callback(API_CALLBACK)
			.build();
	}
	
	/**
	 * Step 2: Get the request token.
	 */
	public void getRequestToken() {
		requestToken = service.getRequestToken();
	}
	
	/**
	 * Step 3: Making the user validate your request token.
	 */
	public String getAuthUrl() {
		if (requestToken == null) {
			throw new IllegalStateException("Request token not found!");
		}
		
		authUrl = service.getAuthorizationUrl(requestToken);
		return authUrl;
	}
	
	/**
	 * Step 4: Get the access Token.
	 * 
	 * @param verifier Provided by the service and entered by the user
	 */
	public void getAccessToken(String verifier) {
		if (requestToken == null) {
			throw new IllegalStateException("Request token not found!");
		}
		
		accessToken = service.getAccessToken(requestToken, new Verifier(verifier));
		
		System.out.println("Key: " + accessToken.getToken() + "\nSecret: " + accessToken.getSecret() + "\n");
	}
	
	/**
	 * Step 5: Sign and send request.
	 * 
	 * @param verb Request method
	 * @param url Request address
	 * @return New OAuthRequest
	 */
	public Response sendRequest(Verb method, String url) {
		if (accessToken == null) {
			throw new IllegalStateException("Access token not found!");
		}
		
		OAuthRequest request = new OAuthRequest(method, url);
		service.signRequest(accessToken, request);
		return request.send();
	}
	
	/**
	 * Restore a previously used access token.
	 */
	@SuppressWarnings("unused")
	public boolean restoreAccessToken() {
		/* TODO: Restore access token from database. */
		if (true) {
			accessToken = new Token("<access-token>", "<access-secret>");
			return true;
		}
		
		return false;
	}
	
	/**
	 * Resets the stored access token.
	 */
	public void resetAccessToken() {
		/* TODO: Delete access token from database. */
		accessToken = null;
	}
	
}
