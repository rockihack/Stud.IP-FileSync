package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.TreeSync;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;

public class OverviewController extends AbstractController {

	@FXML
	private ProgressIndicator progress;

	@FXML
	private Button sync;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
	}

	@FXML
	public void handleSync() {
		sync.setDisable(true);
		sync.setText("Updating...");

		new Thread(new Runnable() {

			@Override
			public void run() {
				OAuth oauth = OAuth.getInstance();

				String rootDir = Config.getInstance().getRootDirectory();
				if (rootDir != null && oauth.restoreAccessToken()) {
					try (TreeSync tree = new TreeSync(new File(rootDir))) {
						File treeFile = Config.getInstance().openTreeFile();

						tree.setProgress(progress);

						// Update documents.
						tree.build(treeFile);

						// Update sync button.
						Platform.runLater(new Runnable() {

							@Override
							public void run() {
								sync.setText("Downloading...");
							}

						});

						// Download documents.
						tree.sync(treeFile, false);

					} catch (JsonGenerationException | JsonMappingException e) {
						throw new IllegalStateException(e);
					} catch (IOException e) {
						throw new IllegalStateException(e);
					}

				} else {
					OAuth.getInstance().removeAccessToken();

					Platform.runLater(new Runnable() {

						@Override
						public void run() {
							// Redirect to login.
							getMain().setView(Main.OAUTH);
						}

					});
				}

				// Update progress and sync button.
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						progress.setProgress(1);
						sync.setText("Sync");
						sync.setDisable(false);
					}

				});
			}

		}).start();
	}

}