package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.OAuth;
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
		OAuth.getInstance().removeAccessToken();

		// Redirect to login.
		getMain().setView(Main.OAUTH);
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
