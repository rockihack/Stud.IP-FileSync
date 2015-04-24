package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
import javafx.fxml.FXML;

public class OAuthCompleteController extends AbstractController {

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
	}
	
	@FXML
	public void handleNext() {
		getMain().setView(Main.OVERVIEW);
	}
	
}
