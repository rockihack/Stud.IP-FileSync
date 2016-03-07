package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.SimpleAlert;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class OAuthWebviewController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();
	private static final OAuth OAUTH = OAuth.getInstance();

	@FXML
	private WebView webView;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		final WebEngine webEngine = webView.getEngine();
		webEngine.setJavaScriptEnabled(true);

		// On status change event.
		webEngine.setOnStatusChanged(event -> {
			if (event.getEventType() == WebEvent.STATUS_CHANGED) {
				final String url = webEngine.getLocation();
				if (url.contains("oauth_verifier")) {
					getOAuthVerifier(url);
					webEngine.load(StudIPApiProvider.LOGOUT);

				} else if (!url.contains("oauth_token") && !url.contains("logout")) {
					webEngine.load(OAUTH.getAuthUrl());
				}
			}
		});

		Platform.runLater(() -> {
			try {
				// Open oauth authentication url.
				OAUTH.getRequestToken();
				webEngine.load(OAUTH.getAuthUrl());

			} catch (OAuthConnectionException e) {
				SimpleAlert.error("Es konnte keine Verbindung zum Stud.IP Server hergestellt werden!");
				getMain().setPrevView();

			} catch (OAuthException e) {
				// Invalid api key or secret.
				SimpleAlert.error("OAuth Verbindung fehlgeschlagen!");
				Platform.exit();
			}
		});
	}

	/**
	 * Get oauth verifier from url.
	 * 
	 * @param url 
	 */
	private void getOAuthVerifier(final String url) {
		try {
			// Parse oauth verifier.
			final Pattern pattern = Pattern.compile("oauth_verifier=(.+)");
			final Matcher matcher = pattern.matcher(url);

			if (!matcher.find()) {
				return;
			}

			// Authentication succeeded, now get the oauth access token.
			final Token accessToken = OAUTH.getAccessToken(matcher.group(1));

			// Test if access token is valid.
			final User currentUser = RestApi.getUserById(null);

			// Store access token.
			CONFIG.setAccessToken(accessToken, currentUser);

			getMain().setView(Main.SETUP_ROOTDIR);

		} catch (OAuthException | UnauthorizedException | NotFoundException e) {
			OAUTH.removeAccessToken();
			getMain().setView(Main.OAUTH);

		} catch (IOException e) {
			SimpleAlert.exception(e);
		}
	}
}
