package de.uni.hannover.studip.sync.models;

import java.io.IOException;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;

/**
 * OAuth model.
 * 
 * @author Lennart Glauer
 */
public class OAuth {
	
	/**
	 * Singleton instance.
	 */
	private static final OAuth singletonInstance = new OAuth();
	
	/**
	 * Config.
	 */
	private static final Config config = Config.getInstance();

	/**
	 * Singleton instance getter.
	 * 
	 * @return OAuth instance
	 */
	public static OAuth getInstance() {
		return singletonInstance;
	}
	
	/**
	 * Service object.
	 */
	private final OAuthService service;
	
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
			.apiKey(StudIPApiProvider.API_KEY)
			.apiSecret(StudIPApiProvider.API_SECRET)
			.callback(StudIPApiProvider.API_CALLBACK)
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
		
		/* Store access token. */
		try {
			config.setAccessToken(accessToken);
			
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
		request.setConnectionKeepAlive(true);
		service.signRequest(accessToken, request);
		return request.send();
	}
	
	/**
	 * Restore a previously used access token.
	 * 
	 * @return True if the access token could be restored.
	 */
	public boolean restoreAccessToken() {
		accessToken = config.getAccessToken();
		return accessToken != null;
	}
	
	/**
	 * Remove the stored access token.
	 */
	public void removeAccessToken() {
		try {
			config.setAccessToken(new Token("", ""));
			accessToken = null;
			
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
