package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;

public class SettingsController extends AbstractController {
	
	@FXML
	private Button logout;

	@FXML
	private Label user;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		try {
			Config config = Config.getInstance();
			OAuth oauth = OAuth.getInstance();
			oauth.restoreAccessToken();
			user.setText("( Eingeloggt als " + config.getFirstName() + " " + config.getLastName() + ", " + config.getUserName() + " )");
			logout.setDisable(false);

		} catch (UnauthorizedException e) {}
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
