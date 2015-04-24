package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.*;
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
						onload(webEngine.getLocation());
					}
				}
				
			});
		
		// Open oauth authentication url.
		OAuth oauth = OAuth.getInstance();
		// TODO: Exception (next, prev, next)!
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
			// Get oauth access token.
			try {
				OAuth oauth = OAuth.getInstance();
				oauth.getAccessToken(matcher.group(1));

			} catch (UnauthorizedException e) {
				// TODO
				e.printStackTrace();
			} catch (NotFoundException e) {
				// TODO
				e.printStackTrace();
			} catch (IOException e) {
				// TODO
				e.printStackTrace();
			}
			
			// Redirect to overview.
			getMain().setView(Main.OAUTH_COMPLETE);
		}
	}
	
	@FXML
	public void handlePrev() {
		getMain().setView(Main.OAUTH);
	}
}
