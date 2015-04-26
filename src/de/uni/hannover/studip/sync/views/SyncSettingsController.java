package de.uni.hannover.studip.sync.views;

import java.io.IOException;

import de.uni.hannover.studip.sync.models.Config;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;

public class SyncSettingsController extends AbstractController {

	@FXML
	private CheckBox overwriteCheckBox;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		Config config = Config.getInstance();

		overwriteCheckBox.setSelected(config.getOverwriteFiles());
	}

	@FXML
	public void handleChange() {
		Config config = Config.getInstance();

		try {
			config.setOverwriteFiles(overwriteCheckBox.isSelected());

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
