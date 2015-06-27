package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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

/**
 * 
 * @author Lennart Glauer
 *
 */
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
		final String rootDir = Config.getInstance().getRootDirectory();
		if (rootDir == null || rootDir.isEmpty()) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Kein Ziel Ordner gewählt.");
			alert.showAndWait();
			return;
		}

		try {
			if (!FileBrowser.open(Paths.get(rootDir))) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Ziel Ordner kann nicht geöffnet werden.");
				alert.showAndWait();
			}

		} catch (IOException e) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Ziel Ordner wurde nicht gefunden.");
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
	 * Help -> Help.
	 */
	@FXML
	public void handleHelp() {
		// Redirect to help.
		//getMain().setView(Main.HELP);
		final Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Info");
		alert.setHeaderText(null);
		alert.setContentText("Keine Hilfe enthalten.");
		alert.showAndWait();
	}

	/**
	 * Help -> Update.
	 */
	@FXML
	public void handleUpdateSeminars() {
		final Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Bestätigen");
		confirm.setHeaderText(null);
		confirm.setContentText("Diese Funktion sollte nur zu Beginn eines Semesters genutzt werden, "
				+ "nachdem Sie sich in neue Veranstaltungen eingeschrieben haben. "
				+ "Möchten Sie fortfahren?");
		confirm.getDialogPane().setPrefSize(400, 150);
		final Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
		yesButton.setDefaultButton(false);
		final Button cancelButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL);
		cancelButton.setDefaultButton(true);
		final Optional<ButtonType> result = confirm.showAndWait();

		if (result.get() == ButtonType.OK) {
			try {
				// Signal the sync routine to rebuild the tree.
				Files.deleteIfExists(Config.openTreeFile());

				// Redirect to overview.
				getMain().setView(Main.OVERVIEW);

				// Start the sync.
				final OverviewController overview = (OverviewController) getMain().getController();
				overview.handleSync();

			} catch (IOException e) {
				final Alert alert = new Alert(AlertType.ERROR);
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
