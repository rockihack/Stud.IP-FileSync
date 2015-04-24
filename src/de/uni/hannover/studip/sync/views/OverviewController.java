package de.uni.hannover.studip.sync.views;

import javafx.fxml.FXML;
import javafx.scene.control.ProgressIndicator;

public class OverviewController extends AbstractController {
	
	@FXML
	private ProgressIndicator progress;
	
	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
	}
	
	@FXML
	public void handleSync() {
		System.out.println("Sync button pressed.");
		// TODO
	}
	
}
