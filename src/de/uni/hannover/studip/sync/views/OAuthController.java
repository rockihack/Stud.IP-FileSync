package de.uni.hannover.studip.sync.views;

import javax.swing.JOptionPane;

import de.uni.hannover.studip.sync.Main;
import javafx.fxml.FXML;

public class OAuthController extends AbstractController {
	
	@FXML
	public void handleNext() {
		try {
			getMain().setView(Main.OAUTH_WEBVIEW);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Keine Verbindung zum Internet möglich!", "Fehler", JOptionPane.ERROR_MESSAGE);
		}
	}
	
}
