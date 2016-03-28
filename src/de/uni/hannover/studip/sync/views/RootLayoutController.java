package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.Export;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import de.uni.hannover.studip.sync.utils.SimpleAlert;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.stage.DirectoryChooser;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class RootLayoutController extends AbstractController {

	@FXML
	private MenuBar menu;

	/**
	 * Get menu instance.
	 * 
	 * @return
	 */
	public MenuBar getMenu() {
		return menu;
	}

	/**
	 * File -> New documents.
	 */
	@FXML
	public void handleNewDocuments() {
		// Redirect to new documents.
		getMain().setView(Main.NEW_DOCUMENTS);
	}

	/**
	 * File -> Open folder.
	 */
	@FXML
	public void handleOpenFolder() {
		final String rootDir = Config.getInstance().getRootDirectory();
		if (rootDir == null || rootDir.isEmpty()) {
			SimpleAlert.error("Kein Ziel Ordner gewählt.");
			return;
		}

		try {
			if (!FileBrowser.open(Paths.get(rootDir))) {
				SimpleAlert.error("Ziel Ordner kann nicht geöffnet werden.");
			}

		} catch (IOException e) {
			SimpleAlert.exception(e);
		}
	}

	/**
	 * File -> Settings.
	 */
	@FXML
	public void handleSettings() {
		// Redirect to settings.
		getMain().setView(Main.SETTINGS);
	}

	/**
	 * File -> Exit.
	 */
	@FXML
	public void handleExit() {
		Main.exitPending = true;
		Platform.exit();
	}

	/**
	 * Help -> Help.
	 */
	@FXML
	public void handleHelp() {
		// Redirect to help.
		getMain().setView(Main.HELP);
	}

	/**
	 * Help -> Export Materialsammlung.
	 */
	@FXML
	public void handleExportMat() {
		/* Root directory. */
		final String rootDir = Config.getInstance().getRootDirectory();
		if (rootDir == null || rootDir.isEmpty()) {
			SimpleAlert.error("Kein Ziel Ordner gewählt.");
			return;
		}

		/* Export directory. */
		final DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Export Ordner wählen");
		chooser.setInitialDirectory(new File(System.getProperty("user.home")));

		final File exportDir = chooser.showDialog(getMain().getPrimaryStage());
		if (exportDir == null) {
			return;
		}

		(new Thread(() -> {
			if (!Main.TREE_LOCK.tryLock()) {
				return;
			}

			try {
				final Path rootDirPath = Paths.get(rootDir);
				final Path exportDirPath = exportDir.toPath();
				Export.exportMat(rootDirPath, exportDirPath);
				FileBrowser.open(exportDirPath);

			} catch (NoSuchFileException | JsonParseException | JsonMappingException e) {
				Platform.runLater(() -> {
					final ButtonType result = SimpleAlert.confirm("Keine Dokumente gefunden.\nMöchten Sie Ihre Dokumente jetzt synchronisieren?");
					if (result == ButtonType.OK) {
						// Redirect to overview.
						getMain().setView(Main.OVERVIEW);

						// Start the sync.
						final OverviewController overview = (OverviewController) getMain().getController();
						overview.handleSync();
					}
				});

			} catch (IOException e) {
				Platform.runLater(() -> SimpleAlert.exception(e));

			} finally {
				Main.TREE_LOCK.unlock();
			}
		})).start();
	}

	/**
	 * Help -> Update.
	 */
	@FXML
	public void handleUpdateSeminars() {
		final ButtonType result = SimpleAlert.confirm("Diese Funktion sollte nur zu Beginn eines Semesters genutzt werden, "
				+ "nachdem Sie sich in neue Veranstaltungen eingeschrieben haben. "
				+ "Möchten Sie fortfahren?");
		if (result == ButtonType.OK) {
			Main.TREE_LOCK.lock();
			try {
				// Signal the sync routine to rebuild the tree.
				Files.deleteIfExists(Config.openTreeFile());

			} catch (IOException e) {
				SimpleAlert.exception(e);

			} finally {
				Main.TREE_LOCK.unlock();
			}

			// Redirect to overview.
			getMain().setView(Main.OVERVIEW);

			// Start the sync.
			final OverviewController overview = (OverviewController) getMain().getController();
			overview.handleSync();
		}
	}

	/**
	 * Help -> About.
	 */
	@FXML
	public void handleAbout() {
		getMain().setView(Main.ABOUT);
	}
}
