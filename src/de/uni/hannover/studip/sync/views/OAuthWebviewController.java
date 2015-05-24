package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.model.Token;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import javafx.application.Platform;
import javafx.concurrent.Worker.State;
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
			if (event.getEventType() == WebEvent.STATUS_CHANGED && webEngine.getLocation().contains("oauth_verifier")) {
				getOAuthVerifier(webEngine);
			}
		});

		// On load event.
		webEngine.getLoadWorker().stateProperty().addListener(
			(observableValue, oldState, newState) -> {
				if (newState == State.SUCCEEDED) {
					// Scroll to login box.
					webEngine.executeScript("if (typeof $ === 'function') {"
						+ "var login = $('form[name=\"login\"]');"
						+ "if (login.length) {"
							+ "$('html, body').animate({"
								+ "scrollTop: login.offset().top - 100,"
								+ "scrollLeft: login.offset().left - 100"
							+ "}, 500);"
						+ "}"
					+ "}");
				}
			});

		try {
			// Open oauth authentication url.
			OAUTH.getRequestToken();
			webEngine.load(OAUTH.getAuthUrl());

		} catch (OAuthConnectionException e) {
			Platform.runLater(() -> {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Keine Verbindung zum Internet möglich!");
				alert.showAndWait();

				getMain().setPrevView();
			});
		}
	}

	/**
	 * Get oauth verifier from url.
	 * 
	 * @param url 
	 */
	private void getOAuthVerifier(final WebEngine engine) {
		// Parse oauth verifier.
		final Pattern pattern = Pattern.compile("oauth_verifier=(.+)");
		final Matcher matcher = pattern.matcher(engine.getLocation());

		if (matcher.find()) {
			try {
				// Authentication succeeded, now get the oauth access token.
				final Token accessToken = OAUTH.getAccessToken(matcher.group(1));

				// Test if access token is valid.
				final User currentUser = RestApi.getUserById(null);

				// Store access token.
				CONFIG.setAccessToken(accessToken, currentUser);

				final String rootDir = CONFIG.getRootDirectory();
				getMain().setView(
						rootDir != null && new File(rootDir).exists()
						? Main.OVERVIEW
						: Main.OAUTH_COMPLETE);

				// End login session.
				engine.load(StudIPApiProvider.LOGOUT);

			} catch (UnauthorizedException | NotFoundException e) {
				OAUTH.removeAccessToken();

				// Redirect to login.
				getMain().setView(Main.OAUTH);

			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}
}
