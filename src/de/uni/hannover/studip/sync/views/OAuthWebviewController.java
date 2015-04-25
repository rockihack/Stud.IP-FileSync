package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class OAuthWebviewController extends AbstractController {

	@FXML
	private WebView webView;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		WebEngine webEngine = webView.getEngine();
		webEngine.setJavaScriptEnabled(true);
		webEngine.getLoadWorker().stateProperty().addListener(
			new ChangeListener<State>() {

				@SuppressWarnings("rawtypes")
				@Override
				public void changed(ObservableValue arg0, State oldState, State newState) {
					if (newState == State.SUCCEEDED) {
						// Scroll to login box.
						webEngine.executeScript(
								"var login = $('form[name=\"login\"]');" +
								"if (login.length)" +
									"$('html, body').animate({"
											+ "scrollTop: login.offset().top - 150,"
											+ "scrollLeft: login.offset().left - 100"
									+ "}, 500);");

						onload(webEngine.getLocation());
					}
				}

			});

		// Open oauth authentication url.
		OAuth oauth = OAuth.getInstance();
		oauth.getRequestToken();
		webEngine.load(oauth.getAuthUrl());
	}

	/**
	 * WebEngine window onload callback.
	 * 
	 * @param url 
	 */
	private void onload(String url) {
		// Parse oauth verifier.
		Pattern pattern = Pattern.compile("oauth_verifier=(.+)");
		Matcher matcher = pattern.matcher(url);

		if (matcher.find()) {
			// Authentication succeeded, get the oauth access token.
			try {
				OAuth oauth = OAuth.getInstance();
				oauth.getAccessToken(matcher.group(1));

				String rootDir = Config.getInstance().getRootDirectory();
				getMain().setView(
						rootDir != null && new File(rootDir).exists()
						? Main.OVERVIEW
						: Main.OAUTH_COMPLETE);

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
