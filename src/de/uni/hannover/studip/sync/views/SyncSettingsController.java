package de.uni.hannover.studip.sync.views;

import java.io.IOException;

import de.uni.hannover.studip.sync.models.Config;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

public class SyncSettingsController extends AbstractController {

	@FXML
	private ChoiceBox<String> overwriteChoicebox;

	@FXML
	private ChoiceBox<String> downloadAllSemestersChoicebox;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		Config config = Config.getInstance();

		overwriteChoicebox.getSelectionModel().select(config.getOverwriteFiles() ? 0 : 1);
		downloadAllSemestersChoicebox.getSelectionModel().select(config.getDownloadAllSemesters() ? 0 : 1);

		overwriteChoicebox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number oldValue, Number newValue) {
				if (!oldValue.equals(newValue)) {
					try {
						config.setOverwriteFiles(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		});

		downloadAllSemestersChoicebox.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0, Number oldValue, Number newValue) {
				if (!oldValue.equals(newValue)) {
					try {
						config.setDownloadAllSemesters(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			}
		});
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
