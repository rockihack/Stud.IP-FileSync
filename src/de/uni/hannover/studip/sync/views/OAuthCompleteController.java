package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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

	private File dir;

	@FXML
	private Button next;

	@FXML
	public void handleDest() {
		final DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Ziel Ordner wählen");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));

		dir = chooser.showDialog(getMain().getPrimaryStage());
		next.setDisable(dir == null);
	}

	@FXML
	public void handleNext() {
		try {
			if (dir == null) {
				return;
			}

			final Path rootDir = dir.toPath();

			if (!Files.isDirectory(rootDir)) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Kein Ordner gewählt.");
				alert.showAndWait();
				return;
			}

			if (!Files.isReadable(rootDir) || !Files.isWritable(rootDir)) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Keine Lese/Schreib Berechtigung.");
				alert.showAndWait();
				return;
			}

			// Store root directory.
			Config.getInstance().setRootDirectory(rootDir.toAbsolutePath().toString());

			getMain().setView(Main.SETUP_STRUCTURE);

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}