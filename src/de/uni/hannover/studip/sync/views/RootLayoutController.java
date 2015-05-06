package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Alert.AlertType;

public class RootLayoutController extends AbstractController {

	@FXML
	private MenuBar menu;

	/**
	 * Get menu instance.
	 * 
	 * @return
	 */
	public MenuBar getMenu() {
		return menu;
	}

	/**
	 * File -> New documents.
	 */
	@FXML
	public void handleNewDocuments() {
		// Redirect to new documents.
		getMain().setView(Main.NEW_DOCUMENTS);
	}

	/**
	 * File -> Open folder.
	 */
	@FXML
	public void handleOpenFolder() {
		String rootDir = Config.getInstance().getRootDirectory();
		if (rootDir != null) {
			if (!FileBrowser.open(new File(rootDir))) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Not supported.");
				alert.showAndWait();
			}

		} else {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Kein Ziel Ordner gewählt.");
			alert.showAndWait();
		}
	}

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
	 * Help -> Update.
	 */
	@FXML
	public void handleUpdateSeminars() {
		Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Bestätigen");
		confirm.setHeaderText(null);
		confirm.setContentText("Diese Funktion sollte nur zu Beginn eines Semesters genutzt werden, "
				+ "nachdem Sie sich in neue Veranstaltungen eingeschrieben haben. "
				+ "Möchten Sie fortfahren?");
		Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
		yesButton.setDefaultButton(false);
		Button cancelButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL);
		cancelButton.setDefaultButton(true);
		Optional<ButtonType> result = confirm.showAndWait();

		if (result.get() == ButtonType.OK) {
			try {
				// Delete the tree file to signal the sync routine,
				// to rebuild the tree.
				Config.getInstance().openTreeFile().delete();

				// Redirect to overview.
				getMain().setView(Main.OVERVIEW);

				// Start the sync.
				OverviewController overview = (OverviewController) getMain().getController();
				overview.handleSync();

			} catch (IOException e) {
				Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText(e.getMessage());
				alert.showAndWait();
			}
		}
	}

	/**
	 * Help -> About.
	 */
	@FXML
	public void handleAbout() {
		getMain().setView(Main.ABOUT);
	}

}
