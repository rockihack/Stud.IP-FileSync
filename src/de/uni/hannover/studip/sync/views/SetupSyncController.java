package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
import javafx.fxml.FXML;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SetupSyncController extends SyncSettingsController {

	@FXML
	public void handleNext() {
		getMain().setView(Main.OVERVIEW);
	}

}
