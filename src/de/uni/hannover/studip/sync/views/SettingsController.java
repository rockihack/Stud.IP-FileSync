package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;

public class SettingsController extends AbstractController {
	
	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		// TODO: Enable logout button, if logged in
	}
	
	@FXML
	public void handleLogout() {
		OAuth.getInstance().removeAccessToken();
		
		getMain().setView(Main.OAUTH);
	}
	
	@FXML
	public void handleRootDir() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Ziel Ordner wählen");

		File rootDir = chooser.showDialog(null);
		if (rootDir != null) {
			// Store new root directory.
			try {
				Config.getInstance().setRootDirectory(rootDir.getAbsolutePath());

			} catch (JsonGenerationException | JsonMappingException e) {
				throw new IllegalStateException(e);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
	}
	
	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
