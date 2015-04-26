package de.uni.hannover.studip.sync.views;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import javafx.application.Platform;
import javafx.fxml.FXML;

public class RootLayoutController extends AbstractController {

	/**
	 * File -> Open.
	 */
	@FXML
	public void handleOpen() {
		try {
			String rootDir = Config.getInstance().getRootDirectory();
			if (rootDir != null) {
				Desktop.getDesktop().open(new File(rootDir));

			} else {
				JOptionPane.showMessageDialog(null, "Kein Ziel Ordner gewählt.", "Fehler", JOptionPane.ERROR_MESSAGE);
			}

		} catch (IOException | UnsupportedOperationException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
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
		Platform.exit();
	}

	/**
	 * Help -> Update.
	 */
	@FXML
	public void handleUpdateSeminars() {
		try {
			Config.getInstance().openTreeFile().delete();

			getMain().setView(Main.OVERVIEW);

			OverviewController overview = (OverviewController) getMain().getController();
			overview.handleSync();

		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
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
