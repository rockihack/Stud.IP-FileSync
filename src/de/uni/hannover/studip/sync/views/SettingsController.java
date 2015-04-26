package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

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
	private Button logoutButton;

	@FXML
	private Label userLabel;

	@FXML
	private Label rootDirLabel;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		Config config = Config.getInstance();

		// Stud.IP Account.
		try {
			OAuth oauth = OAuth.getInstance();
			oauth.restoreAccessToken();

			// User has an access token, we do not check if it's valid here.
			userLabel.setText("( Eingeloggt als " + config.getFirstName() + " " + config.getLastName() + ", " + config.getUserName() + " )");
			logoutButton.setDisable(false);

		} catch (UnauthorizedException e) {}

		// Root dir.
		setRootDirLabel(config.getRootDirectory());
	}

	@FXML
	public void handleLogout() {
		// Delete access token and update oauth config file.
		OAuth.getInstance().removeAccessToken();

		// Redirect to login.
		getMain().setView(Main.OAUTH);
	}

	@FXML
	public void handleRootDir() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Ziel Ordner wählen");
		chooser.setInitialDirectory(new File(Config.getInstance().getHomeDirectory()));

		File rootDir = chooser.showDialog(null);
		if (rootDir != null) {
			try {
				Config.getInstance().setRootDirectory(rootDir.getAbsolutePath());
				setRootDirLabel(rootDir.getAbsolutePath());

			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	
	@FXML
	public void handleSyncOptions() {
		// Redirect to sync settings.
		getMain().setView(Main.SYNC_SETTINGS);
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

	private void setRootDirLabel(String rootDir) {
		if (rootDir != null && new File(rootDir).exists()) {
			rootDirLabel.setText("( " + rootDir + " )");
		}
	}

}
