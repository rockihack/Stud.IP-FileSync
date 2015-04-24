package de.uni.hannover.studip.sync.views;

import javafx.application.Platform;
import javafx.fxml.FXML;

public class RootLayoutController extends AbstractController {
	
	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
	}
	
	/**
	 * File -> Logout.
	 */
	@FXML
	public void handleLogout() {
		// TODO
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
		// TODO
	}
	
}
