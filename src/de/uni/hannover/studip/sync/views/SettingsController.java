package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.utils.SimpleAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
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

	@FXML
	private Label structureLabel;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		// Stud.IP Account.
		if (OAUTH.restoreAccessToken()) {
			logoutButton.setDisable(false);

			final String firstName = CONFIG.getFirstName();
			final String lastName = CONFIG.getLastName();
			final String userName = CONFIG.getUserName();
			if (firstName != null && lastName != null && userName != null) {
				userLabel.setText(String.format(Locale.GERMANY, "Eingeloggt als %s %s, %s", firstName, lastName, userName));
			}

		} else {
			logoutButton.setDisable(true);
		}

		// Root dir.
		setRootDirLabel(CONFIG.getRootDirectory());

		// Folder structure.
		structureLabel.setText("Aktuell: " + CONFIG.getFolderStructure());
	}

	@FXML
	public void handleLogout() {
		final ButtonType result = SimpleAlert.confirm("Möchten Sie sich wirklich ausloggen?");
		if (result == ButtonType.OK) {
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
			SimpleAlert.error("Kein Ordner gewählt.");
			return;
		}
		if (!Files.isReadable(rootDir) || !Files.isWritable(rootDir)) {
			SimpleAlert.error("Keine Lese/Schreib Berechtigung.");
			return;
		}

		try {
			CONFIG.setRootDirectory(rootDir.toAbsolutePath().toString());
			setRootDirLabel(CONFIG.getRootDirectory());

		} catch (IOException e) {
			SimpleAlert.exception(e);
		}
	}

	@FXML
	public void handleStructureOptions() {
		// Redirect to structure settings.
		getMain().setView(Main.STRUCTURE_SETTINGS);
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
