package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class OAuthCompleteController extends AbstractController {

	private File rootDir;

	@FXML
	private Button next;

	@FXML
	public void handleDest() {
		final DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Ziel Ordner wählen");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));

		rootDir = chooser.showDialog(getMain().getPrimaryStage());
		next.setDisable(rootDir == null || !rootDir.isDirectory());
	}

	@FXML
	public void handleNext() {
		try {
			if (rootDir != null && rootDir.isDirectory()) {
				if (!rootDir.canRead() || !rootDir.canWrite()) {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Fehler");
					alert.setHeaderText(null);
					alert.setContentText("Keine Lese/Schreib Berechtigung.");
					alert.showAndWait();
					return;
				}

				// Store root directory.
				Config.getInstance().setRootDirectory(rootDir.getAbsolutePath());

				// Redirect to overview.
				getMain().setView(Main.OVERVIEW);
			}
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}