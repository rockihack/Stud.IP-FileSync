package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.DirectoryChooser;

public class OAuthCompleteController extends AbstractController {

	private File rootDir;

	@FXML
	private Button next;

	@FXML
	public void handleDest() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Ziel Ordner wählen");

		rootDir = chooser.showDialog(null);
		next.setDisable(rootDir == null);
	}

	@FXML
	public void handleNext() {
		try {
			// Store root directory.
			Config.getInstance().setRootDirectory(rootDir.getAbsolutePath());

			// Redirect to overview.
			getMain().setView(Main.OVERVIEW);

		} catch (JsonGenerationException | JsonMappingException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

}