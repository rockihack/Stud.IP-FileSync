package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class RootLayoutController extends AbstractController {
	
	/**
	 * File -> Settings.
	 */
	@FXML
	public void handleSettings() {
		// Redirect to settings.
		getMain().setView(Main.SETTINGS);
	}

	/**
	 * File -> Exit.
	 */
	@FXML
	public void handleExit() {
		Platform.exit();
	}
	
	/**
	 * Help -> About.
	 */
	@FXML
	public void handleAbout() {
		getMain().setView(Main.ABOUT);
	}
	
}
