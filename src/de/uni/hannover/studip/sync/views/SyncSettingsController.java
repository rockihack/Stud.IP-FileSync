package de.uni.hannover.studip.sync.views;

import java.io.IOException;

import de.uni.hannover.studip.sync.models.Config;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SyncSettingsController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();

	@FXML
	private ChoiceBox<String> downloadAllSemestersChoicebox;

	@FXML
	private ChoiceBox<String> overwriteChoicebox;

	@FXML
	private ChoiceBox<String> replaceWhitespacesChoicebox;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		downloadAllSemestersChoicebox.getSelectionModel().select(CONFIG.isDownloadAllSemesters() ? 0 : 1);
		overwriteChoicebox.getSelectionModel().select(CONFIG.isOverwriteFiles() ? 0 : 1);
		replaceWhitespacesChoicebox.getSelectionModel().select(CONFIG.getReplaceWhitespaces());

		downloadAllSemestersChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				try {
					CONFIG.setDownloadAllSemesters(newValue.intValue() == 0);

				} catch (IOException e) {
					downloadAllSemestersChoicebox.getSelectionModel().selectPrevious();
				}
			});

		overwriteChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				try {
					CONFIG.setOverwriteFiles(newValue.intValue() == 0);

				} catch (IOException e) {
					overwriteChoicebox.getSelectionModel().selectPrevious();
				}
			});

		replaceWhitespacesChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				try {
					CONFIG.setReplaceWhitespaces(newValue.intValue());

				} catch (IOException e) {
					replaceWhitespacesChoicebox.getSelectionModel().selectPrevious();
				}
			});
	}
}
