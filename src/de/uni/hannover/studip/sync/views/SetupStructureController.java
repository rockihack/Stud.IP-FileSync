package de.uni.hannover.studip.sync.views;

import javafx.fxml.FXML;
import de.uni.hannover.studip.sync.Main;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SetupStructureController extends StructureSettingsController {

	@FXML
	public void handleNext() {
		handleSave();

		getMain().setView(Main.SETUP_SYNC);
	}
}
