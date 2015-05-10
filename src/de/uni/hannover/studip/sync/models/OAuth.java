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
public final class OAuth {

	/**
	 * Singleton instance.
	 */
	private static final OAuth INSTANCE = new OAuth();

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
	private volatile Token accessToken;

	/**
	 * Current state.
	 */
	private volatile OAuthState state;

	/**
	 * Singleton instance getter.
	 * 
	 * @return OAuth instance
	 */
	public static OAuth getInstance() {
		return INSTANCE;
	}

	/**
	 * State enum.
	 */
	private enum OAuthState {
		GET_REQUEST_TOKEN,
		GET_ACCESS_TOKEN,
		READY
	}

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

		state = OAuthState.GET_REQUEST_TOKEN;
	}
	
	/**
	 * Step 2: Get the request token.
	 */
	public synchronized void getRequestToken() throws OAuthConnectionException {
		if (state == OAuthState.GET_REQUEST_TOKEN) {
			requestToken = service.getRequestToken();
			state = OAuthState.GET_ACCESS_TOKEN;
		}
	}

	/**
	 * Step 3: Making the user validate your request token.
	 */
	public synchronized String getAuthUrl() {
		if (state != OAuthState.GET_ACCESS_TOKEN) {
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
	public synchronized Token getAccessToken(final String verifier) throws UnauthorizedException, NotFoundException, IOException {
		if (state != OAuthState.GET_ACCESS_TOKEN) {
			throw new IllegalStateException("Request token not found!");
		}

		// Get access token and store it in oauth config file.
		accessToken = service.getAccessToken(requestToken, new Verifier(verifier));
		state = OAuthState.READY;

		return accessToken;
	}
	
	/**
	 * Step 5: Sign and send request.
	 * 
	 * @param verb Request method
	 * @param url Request address
	 * @return New OAuthRequest
	 */
	public Response sendRequest(final Verb method, final String url) {
		if (state != OAuthState.READY) {
			throw new IllegalStateException("Access token not found!");
		}

		final OAuthRequest request = new OAuthRequest(method, url);
		request.setConnectionKeepAlive(true);
		service.signRequest(accessToken, request);
		return request.send();
	}

	/**
	 * Restore a previously used access token.
	 * 
	 * @return True if the access token could be restored.
	 * @throws UnauthorizedException 
	 */
	public synchronized void restoreAccessToken() throws UnauthorizedException {
		accessToken = Config.getInstance().getAccessToken();
		state = OAuthState.READY;
	}

	/**
	 * Remove access token.
	 */
	public synchronized void removeAccessToken() {
		try {
			Config.getInstance().initOAuthFile();
			state = OAuthState.GET_REQUEST_TOKEN;

		} catch (IOException | InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
