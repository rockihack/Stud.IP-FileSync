package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.scribe.exceptions.OAuthConnectionException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
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

	/**
	 * Logger instance.
	 */
	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private static final Config CONFIG = Config.getInstance();

	private static final OAuth OAUTH = OAuth.getInstance();

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
					OAUTH.restoreAccessToken();

					final String rootDir = CONFIG.getRootDirectory();
					if (rootDir == null) {
						throw new IOException("Kein Ziel Ordner gewählt.");
					}

					try (TreeSync tree = new TreeSync(new File(rootDir))) {
						final File treeFile = Config.openTreeFile();
						int numberOfRequests;

						tree.setProgress(progress, progressLabel);

						// Update documents.
						try {
							numberOfRequests = tree.update(treeFile, false);

						} catch (JsonParseException | JsonMappingException e) {
							// Invalid tree file, lets build a new one.
							numberOfRequests = tree.build(treeFile);
						}

						// Update sync button.
						Platform.runLater(() -> syncButton.setText("Downloading..."));

						// Download documents.
						numberOfRequests += tree.sync(treeFile, CONFIG.isDownloadAllSemesters());

						LOG.info("Number of requests: " + numberOfRequests);
					}

				} catch (UnauthorizedException e) {
					OAUTH.removeAccessToken();

					Platform.runLater(() -> getMain().setView(Main.OAUTH));

				} catch (IOException | OAuthConnectionException e) {
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