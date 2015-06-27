package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SettingsController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();

	private static final OAuth OAUTH = OAuth.getInstance();

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
		// Stud.IP Account.
		try {
			OAUTH.restoreAccessToken();

			// User has an access token, we do not check if it's valid here.

			if (CONFIG.getFirstName() != null && CONFIG.getLastName() != null && CONFIG.getUserName() != null) {
				userLabel.setText("Eingeloggt als " + CONFIG.getFirstName() + " " + CONFIG.getLastName() + ", " + CONFIG.getUserName());
			}

			logoutButton.setDisable(false);

		} catch (UnauthorizedException e) {
			// Not logged in.
			logoutButton.setDisable(true);
		}

		// Root dir.
		setRootDirLabel(CONFIG.getRootDirectory());
	}

	@FXML
	public void handleLogout() {
		final Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Bestätigen");
		confirm.setHeaderText(null);
		confirm.setContentText("Möchten Sie sich wirklich ausloggen?");
		final Optional<ButtonType> result = confirm.showAndWait();

		if (result.get() == ButtonType.OK) {
			// Delete access token and update oauth config file.
			OAUTH.removeAccessToken();

			// Redirect to login.
			getMain().setView(Main.OAUTH);
		}
	}

	@FXML
	public void handleRootDir() {
		final DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Ziel Ordner wählen");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));

		final File dir = chooser.showDialog(getMain().getPrimaryStage());
		if (dir == null) {
			return;
		}

		final Path rootDir = dir.toPath();

		if (!Files.isDirectory(rootDir)) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Kein Ordner.");
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

		try {
			CONFIG.setRootDirectory(rootDir.toAbsolutePath().toString());
			setRootDirLabel(CONFIG.getRootDirectory());

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	@FXML
	public void handleSyncOptions() {
		// Redirect to sync settings.
		getMain().setView(Main.SYNC_SETTINGS);
	}

	private void setRootDirLabel(final String rootDir) {
		if (rootDir != null) {
			rootDirLabel.setText("Aktuell: " + rootDir);
		}
	}

}
