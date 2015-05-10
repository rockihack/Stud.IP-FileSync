package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scribe.model.Token;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class OAuthWebviewController extends AbstractController {

	@FXML
	private WebView webView;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		final WebEngine webEngine = webView.getEngine();
		webEngine.setJavaScriptEnabled(true);
		webEngine.getLoadWorker().stateProperty().addListener(
			(observableValue, oldState, newState) -> {
				if (newState == State.SUCCEEDED) {
					// Scroll to login box.
					webEngine.executeScript(
							"var login = $('form[name=\"login\"]');" +
							"if (login.length)" +
								"$('html, body').animate({"
										+ "scrollTop: login.offset().top - 100,"
										+ "scrollLeft: login.offset().left - 100"
								+ "}, 500);");

					onload(webEngine);
				}
			});

		// Open oauth authentication url.
		final OAuth oauth = OAuth.getInstance();
		oauth.getRequestToken();
		webEngine.load(oauth.getAuthUrl());
	}

	/**
	 * WebEngine window onload callback.
	 * 
	 * @param url 
	 */
	private void onload(final WebEngine engine) {
		// Parse oauth verifier.
		final Pattern pattern = Pattern.compile("oauth_verifier=(.+)");
		final Matcher matcher = pattern.matcher(engine.getLocation());

		if (matcher.find()) {
			try {
				// Authentication succeeded, now get the oauth access token.
				final Token accessToken = OAuth.getInstance().getAccessToken(matcher.group(1));

				// Test if access token is valid.
				final User currentUser = RestApi.getUserById(null);

				// Store access token.
				Config.getInstance().setAccessToken(accessToken, currentUser);

				final String rootDir = Config.getInstance().getRootDirectory();
				getMain().setView(
						rootDir != null && new File(rootDir).exists()
						? Main.OVERVIEW
						: Main.OAUTH_COMPLETE);

				// End login session.
				engine.load(StudIPApiProvider.LOGOUT);

			} catch (UnauthorizedException | NotFoundException e) {
				OAuth.getInstance().removeAccessToken();

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
