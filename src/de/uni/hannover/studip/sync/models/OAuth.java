package de.uni.hannover.studip.sync.models;

import java.io.IOException;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
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
	public synchronized void getRequestToken() throws OAuthConnectionException {
		if (requestToken == null) {
			requestToken = service.getRequestToken();
		}
	}
	
	/**
	 * Step 3: Making the user validate your request token.
	 */
	public synchronized String getAuthUrl() {
		if (requestToken == null) {
			throw new IllegalStateException("Request token not found!");
		}
		
		return service.getAuthorizationUrl(requestToken);
	}
	
	/**
	 * Step 4: Get the access Token.
	 * 
	 * @param verifier Provided by the service and entered by the user
	 * @throws IOException 
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 */
	public synchronized void getAccessToken(String verifier) throws UnauthorizedException, NotFoundException, IOException {
		if (requestToken == null) {
			throw new IllegalStateException("Request token not found!");
		}

		// Get access token and store it in oauth config file.
		config.setAccessToken(accessToken = service.getAccessToken(requestToken, new Verifier(verifier)));

		// Request token was used and is no longer valid.
		requestToken = null;
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
	public synchronized boolean restoreAccessToken() {
		try {
			accessToken = config.getAccessToken();
			return true;

		} catch(IllegalArgumentException e) {
			// Token can't be null.
			return false;
		}
	}
	
	/**
	 * Remove access token.
	 */
	public synchronized void removeAccessToken() {
		try {
			Config.getInstance().initOAuthFile();
			accessToken = null;

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
