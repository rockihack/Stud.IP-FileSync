package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class OAuthController extends AbstractController {

	@FXML
	public void handleNext() {
		try {
			getMain().setView(Main.OAUTH_WEBVIEW);

		} catch (Exception e) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Keine Verbindung zum Internet möglich!");
			alert.showAndWait();
		}
	}

}
