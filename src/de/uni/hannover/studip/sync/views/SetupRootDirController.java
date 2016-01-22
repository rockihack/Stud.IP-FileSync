package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.SimpleAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SetupRootDirController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();

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
		if (dir == null) {
			return;
		}

		final Path rootDir = dir.toPath();
		if (!Files.isDirectory(rootDir)) {
			SimpleAlert.error("Kein Ordner gewählt.");
			return;
		}
		if (!Files.isReadable(rootDir) || !Files.isWritable(rootDir)) {
			SimpleAlert.error("Keine Lese/Schreib Berechtigung.");
			return;
		}

		try {
			CONFIG.setRootDirectory(rootDir.toAbsolutePath().toString());
			getMain().setView(Main.SETUP_STRUCTURE);

		} catch (IOException e) {
			SimpleAlert.exception(e);
		}
	}
}