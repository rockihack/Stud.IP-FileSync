package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
			final String url = webEngine.getLocation();

			if (event.getEventType() == WebEvent.STATUS_CHANGED && url.contains("oauth_verifier")) {
				getOAuthVerifier(url);

				// End login session.
				webEngine.load(StudIPApiProvider.LOGOUT);
			}
		});

		Platform.runLater(() -> {
			try {
				// Open oauth authentication url.
				OAUTH.getRequestToken();
				webEngine.load(OAUTH.getAuthUrl());

			} catch (OAuthConnectionException e) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Es konnte keine Verbindung zum Stud.IP Server hergestellt werden!");
				alert.showAndWait();

				getMain().setPrevView();

			} catch (OAuthException e) {
				// Invalid api key or secret.
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("OAuth Verbindung fehlgeschlagen!");
				alert.showAndWait();

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

			final String rootDir = CONFIG.getRootDirectory();
			getMain().setView(
					rootDir != null && Files.isDirectory(Paths.get(rootDir))
					? Main.OVERVIEW
					: Main.SETUP_ROOTDIR);

		} catch (OAuthException | UnauthorizedException | NotFoundException e) {
			OAUTH.removeAccessToken();

			// Redirect to login.
			getMain().setView(Main.OAUTH);

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
