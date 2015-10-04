package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
 * @notice Thread safe
 */
public final class OAuth {

	private static final OAuth INSTANCE = new OAuth();
	private static final Config CONFIG = Config.getInstance();

	/**
	 * Service object.
	 */
	private final OAuthService service;

	/**
	 * Reentrant read/write lock.
	 */
	private final ReentrantReadWriteLock lock;

	/**
	 * Request token.
	 */
	private Token requestToken;

	/**
	 * Access token.
	 */
	private Token accessToken;

	/**
	 * Current state.
	 */
	private OAuthState state;

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
	private static enum OAuthState {
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

		lock = new ReentrantReadWriteLock();
		state = OAuthState.GET_REQUEST_TOKEN;

		/*
		 * Force java to use tls 1.2 / 1.1 (update default if jre 1.7 was installed before).
		 * Fixes: SSLException: ssl peer shut down incorrectly.
		 */
		System.setProperty("https.protocols", "TLSv1.1,TLSv1.2");
	}

	/**
	 * Step 2: Get the request token.
	 */
	public void getRequestToken() {
		lock.writeLock().lock();
		try {
			if (state == OAuthState.GET_REQUEST_TOKEN) {
				requestToken = service.getRequestToken();
				state = OAuthState.GET_ACCESS_TOKEN;
			}

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Step 3: Making the user validate your request token.
	 */
	public String getAuthUrl() {
		lock.readLock().lock();
		try {
			if (state != OAuthState.GET_ACCESS_TOKEN) {
				throw new IllegalStateException("Request token not found!");
			}

			return service.getAuthorizationUrl(requestToken);

		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Step 4: Get the access Token.
	 * 
	 * @param verifier Provided by the service and entered by the user
	 */
	public Token getAccessToken(final String verifier) {
		lock.writeLock().lock();
		try {
			if (state != OAuthState.GET_ACCESS_TOKEN) {
				throw new IllegalStateException("Request token not found!");
			}

			accessToken = service.getAccessToken(requestToken, new Verifier(verifier));
			state = OAuthState.READY;

			return accessToken;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Step 5: Sign and send request.
	 * 
	 * @param verb Request method
	 * @param url Request url
	 * @return OAuth response
	 */
	public Response sendRequest(final Verb method, final String url) {
		final OAuthRequest request = new OAuthRequest(method, url);
		request.setConnectTimeout(10, TimeUnit.SECONDS);
		request.setConnectionKeepAlive(true);

		lock.readLock().lock();
		try {
			if (state != OAuthState.READY) {
				throw new IllegalStateException("Access token not found!");
			}

			service.signRequest(accessToken, request);

		} finally {
			lock.readLock().unlock();
		}

		return request.send();
	}

	/**
	 * Restore a previously used access token.
	 * 
	 * @return True, if an access token was restored.
	 */
	public boolean restoreAccessToken() {
		lock.writeLock().lock();
		try {
			if (state != OAuthState.READY) {
				accessToken = CONFIG.getAccessToken();
				state = OAuthState.READY;
			}

			return true;

		} catch (IllegalStateException e) {
			return false;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Remove access token.
	 */
	public void removeAccessToken() {
		try {
			CONFIG.initOAuthFile();

			lock.writeLock().lock();
			try {
				/*
				 * We must not acquire a second request token until
				 * we got a access token for the first one.
				 */
				if (state != OAuthState.GET_ACCESS_TOKEN) {
					state = OAuthState.GET_REQUEST_TOKEN;
				}

			} finally {
				lock.writeLock().unlock();
			}

		} catch (IOException | InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}
}
