package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

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
					OAuth oauth = OAuth.getInstance();
					oauth.restoreAccessToken();

					// Test if access token is valid.
					RestApi.getUserById(null);

					String rootDir = Config.getInstance().getRootDirectory();
					if (rootDir != null) {
						try (TreeSync tree = new TreeSync(new File(rootDir))) {
							File treeFile = Config.openTreeFile();

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
							tree.sync(treeFile, Config.getInstance().getDownloadAllSemesters());
						}

					} else {
						Platform.runLater(() -> {
							Alert alert = new Alert(AlertType.ERROR);
							alert.setTitle("Fehler");
							alert.setHeaderText(null);
							alert.setContentText("Kein Ziel Ordner gewählt.");
							alert.showAndWait();
						});
					}

				} catch (UnauthorizedException | NotFoundException e) {
					OAuth.getInstance().removeAccessToken();

					Platform.runLater(() -> getMain().setView(Main.OAUTH));

				} catch (IOException e) {
					Platform.runLater(() -> {
						Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Fehler");
						alert.setHeaderText(null);
						alert.setContentText(e.getMessage());
						alert.showAndWait();
					});

				} finally {
					// Update progress and sync button.
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