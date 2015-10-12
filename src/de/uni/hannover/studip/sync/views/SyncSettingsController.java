package de.uni.hannover.studip.sync.views;

import java.io.IOException;

import de.uni.hannover.studip.sync.models.Config;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.paint.Color;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SyncSettingsController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();

	@FXML
	private ChoiceBox<String> overwriteChoicebox;

	@FXML
	private ChoiceBox<String> downloadAllSemestersChoicebox;

	@FXML
	private ChoiceBox<String> replaceWhitespacesChoicebox;
	
	@FXML
	private CheckBox foldernameConfig;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		overwriteChoicebox.getSelectionModel().select(CONFIG.isOverwriteFiles() ? 0 : 1);
		downloadAllSemestersChoicebox.getSelectionModel().select(CONFIG.isDownloadAllSemesters() ? 0 : 1);
		replaceWhitespacesChoicebox.getSelectionModel().select(CONFIG.getReplaceWhitespaces());
		foldernameConfig.setSelected(CONFIG.getFoldernameConfig());

		overwriteChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						CONFIG.setOverwriteFiles(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

		downloadAllSemestersChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						CONFIG.setDownloadAllSemesters(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

		replaceWhitespacesChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						CONFIG.setReplaceWhitespaces(newValue.intValue());

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});
		
		foldernameConfig.selectedProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						CONFIG.setFoldernameConfig(newValue.booleanValue());

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
					if(CONFIG.getFoldernameConfig()) {
						foldernameConfig.setTextFill(Color.RED);
						foldernameConfig.setText("bitte neustarten");
					}
				}
			});

	}
}
