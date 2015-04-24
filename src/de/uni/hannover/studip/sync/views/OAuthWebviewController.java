package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
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
		// TODO: Use auth url.
		webEngine.load("https://elearning.uni-hannover.de/index.php?again=yes");
	}
	
	/**
	 * WebEngine window onload callback.
	 * 
	 * @param url 
	 */
	private void onload(String url) {
		if (url.contains("oauth_verifier=")) {
			// OAuth authentication succeeded!
			// TODO: Get access token.
			
			// Redirect to overview.
			getMain().setView(Main.OVERVIEW);
		}
	}
	
	@FXML
	public void handlePrev() {
		getMain().setView(Main.OAUTH);
	}
}
