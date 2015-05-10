package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import org.scribe.exceptions.OAuthConnectionException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.models.TreeSync;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Alert.AlertType;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class OverviewController extends AbstractController {

	@FXML
	protected ProgressIndicator progress;

	@FXML
	protected Label progressLabel;

	@FXML
	protected Button syncButton;

	@FXML
	public void handleSync() {
		getMain().getRootLayoutController().getMenu().setDisable(true);
		syncButton.setDisable(true);
		syncButton.setText("Updating...");

		(new Thread() {
			@Override
			public void run() {
				try {
					final OAuth oauth = OAuth.getInstance();
					oauth.restoreAccessToken();

					// Test if access token is valid.
					RestApi.getUserById(null);

					final String rootDir = Config.getInstance().getRootDirectory();
					if (rootDir == null) {
						throw new IOException("Kein Ziel Ordner gewählt.");
					}

					try (TreeSync tree = new TreeSync(new File(rootDir))) {
						final File treeFile = Config.openTreeFile();

						tree.setProgress(progress, progressLabel);

						// Update documents.
						try {
							tree.update(treeFile, false);

						} catch (JsonParseException | JsonMappingException e) {
							// Invalid tree file, lets build a new one.
							tree.build(treeFile);
						}

						// Update sync button.
						Platform.runLater(() -> syncButton.setText("Downloading..."));

						// Download documents.
						tree.sync(treeFile, Config.getInstance().isDownloadAllSemesters());
					}

				} catch (UnauthorizedException | NotFoundException e) {
					OAuth.getInstance().removeAccessToken();

					Platform.runLater(() -> getMain().setView(Main.OAUTH));

				} catch (IOException | OAuthConnectionException | IllegalStateException e) {
					Platform.runLater(() -> {
						final Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Fehler");
						alert.setHeaderText(null);
						alert.setContentText(e.getMessage());
						alert.showAndWait();
					});

				} finally {
					Platform.runLater(() -> {
						progress.setProgress(1);
						progressLabel.setText("");
						syncButton.setText("Sync");
						syncButton.setDisable(false);
						getMain().getRootLayoutController().getMenu().setDisable(false);
					});
				}
			}
		}).start();
	}

}