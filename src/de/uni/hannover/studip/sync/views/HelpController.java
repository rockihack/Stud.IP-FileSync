package de.uni.hannover.studip.sync.views;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class HelpController extends AbstractController {

	@FXML
	private WebView webView;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		final WebEngine webEngine = webView.getEngine();
		webEngine.load(HelpController.class.getResource("Help.html").toExternalForm());
	}

}