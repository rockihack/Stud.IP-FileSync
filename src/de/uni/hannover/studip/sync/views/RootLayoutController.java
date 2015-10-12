package de.uni.hannover.studip.sync.views;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.filechooser.FileView;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.RenameMap;
import de.uni.hannover.studip.sync.utils.Export;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class RootLayoutController extends AbstractController {
	
	private static final Config CONFIG = Config.getInstance();
	
	private static final RenameMap RENAMEMAP = RenameMap.getInstance();
	private File selFile;
	private JList<FileView> view;
	private int selFileIndex;
	
	@FXML
	private MenuItem folderConf;
	
	@FXML
	public void initialize() {
		if(CONFIG.getFoldernameConfig()) {
			folderConf.setVisible(true);
		}
	}

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
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Kein Ziel Ordner gewählt.");
			alert.showAndWait();
			return;
		}

		try {
			if (!FileBrowser.open(Paths.get(rootDir))) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Ziel Ordner kann nicht geöffnet werden.");
				alert.showAndWait();
			}

		} catch (IOException e) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Ziel Ordner wurde nicht gefunden.");
			alert.showAndWait();
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
		//getMain().setView(Main.HELP);
		final Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Info");
		alert.setHeaderText(null);
		alert.setContentText("Keine Hilfe enthalten.");
		alert.showAndWait();
	}

	/**
	 * Help -> Export Materialsammlung.
	 */
	@FXML
	public void handleExportMat() {
		/* Root directory. */
		final String rootDir = Config.getInstance().getRootDirectory();
		if (rootDir == null || rootDir.isEmpty()) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("Kein Ziel Ordner gewählt.");
			alert.showAndWait();
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
				Export.exportMat(Paths.get(rootDir), exportDir.toPath());

			} catch (NoSuchFileException | JsonParseException | JsonMappingException e) {
				Platform.runLater(() -> {
					final Alert confirm = new Alert(AlertType.CONFIRMATION);
					confirm.setTitle("Bestätigen");
					confirm.setHeaderText(null);
					confirm.setContentText("Keine Dokumente gefunden.\nMöchten Sie Ihre Dokumente jetzt synchronisieren?");
					final Optional<ButtonType> result = confirm.showAndWait();

					if (result.get() == ButtonType.OK) {
						// Redirect to overview.
						getMain().setView(Main.OVERVIEW);

						// Start the sync.
						final OverviewController overview = (OverviewController) getMain().getController();
						overview.handleSync();
					}
				});

			} catch (IOException e) {
				Platform.runLater(() -> {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Fehler");
					alert.setHeaderText(null);
					alert.setContentText(e.getMessage());
					alert.showAndWait();
				});

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
		final Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Bestätigen");
		confirm.setHeaderText(null);
		confirm.setContentText("Diese Funktion sollte nur zu Beginn eines Semesters genutzt werden, "
				+ "nachdem Sie sich in neue Veranstaltungen eingeschrieben haben. "
				+ "Möchten Sie fortfahren?");
		confirm.getDialogPane().setPrefSize(400, 150);
		final Button yesButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.OK);
		yesButton.setDefaultButton(false);
		final Button cancelButton = (Button) confirm.getDialogPane().lookupButton(ButtonType.CANCEL);
		cancelButton.setDefaultButton(true);
		final Optional<ButtonType> result = confirm.showAndWait();

		if (result.get() == ButtonType.OK) {
			try {
				// Signal the sync routine to rebuild the tree.
				Files.deleteIfExists(Config.openTreeFile());

				// Redirect to overview.
				getMain().setView(Main.OVERVIEW);

				// Start the sync.
				final OverviewController overview = (OverviewController) getMain().getController();
				overview.handleSync();

			} catch (IOException e) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText(e.getMessage());
				alert.showAndWait();
			}
		}
	}
	
	/**
	 * @author Tim Kohlmeier
	 * File -> Folder rename.
	 */
	@FXML
	public void handleFolderConf() {
		
        if(CONFIG.getRootDirectory() == null) {
    		JOptionPane op = new JOptionPane("Kein Zielordner in den Einstellungen gew\u00e4hlt!" ,JOptionPane.WARNING_MESSAGE);
    		JDialog dialog = op.createDialog("Kann nicht ausgef\u00fchrt werden!");
    		dialog.setAlwaysOnTop(true); //<-- this line
    		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
    		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    		dialog.setVisible(true);
    		return;
        }

		// disable direct renaming by slow double click
		UIManager.put("FileChooser.readOnly", Boolean.TRUE);
		
		final JFileChooser chooser = new JFileChooser("Verzeichnis oder Datei wählen");
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setApproveButtonText("Umbennen");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        
        final File rootDir = new File(CONFIG.getRootDirectory());
        chooser.setCurrentDirectory(rootDir);
        
        JTextField tf = (JTextField) ((JPanel) ((JPanel) chooser.getComponent(3)).getComponent(0)).getComponent(1);
        
        // Hide FileType
        JPanel fileType = (JPanel) ((JPanel) chooser.getComponent(3)).getComponent(2);
        fileType.setVisible(false);
        
        // initialize
        selFileIndex = -1;
        
        tf.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				
				if(selFileIndex != -1) {
					view.setSelectedIndex(selFileIndex);
				} else {
					tf.setForeground(Color.RED);
					tf.setText("bitte w\u00e4hle eine Datei oder Verzeichnis");
					tf.setFocusable(false);
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				tf.setFocusable(true);
			}
        	
        });

        chooser.addPropertyChangeListener(new PropertyChangeListener() {
            @SuppressWarnings("unchecked")
			public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
            		System.out.println("A");
                	if(chooser.getSelectedFile() != null && (chooser.getSelectedFile().exists() || chooser.getSelectedFile().isDirectory())) {
                		selFile = chooser.getSelectedFile();
                		System.out.println(chooser.getSelectedFile().getAbsolutePath());
                		view = (JList<FileView>) ((JViewport) ((JScrollPane) ((JPanel) ((JPanel) chooser.getComponent(2)).getComponent(0)).getComponent(0)).getComponent(0)).getComponent(0);
                        selFileIndex = view.getSelectedIndex();
    					tf.setForeground(Color.BLACK);
                		System.out.println("B");
                	}
                    
                } else if (e.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                	chooser.setSelectedFile(null);
            		selFile = null;
            		selFileIndex = -1;
            		// let select file warning appear
            		tf.requestFocus();
                }
            }
        });

        chooser.setVisible(true);
        boolean chooserEnabled = true;
        
        while(chooserEnabled) {
	        final int result = chooser.showOpenDialog(null);
	        if(result == JFileChooser.CANCEL_OPTION) {
	        	chooserEnabled = false;
	        } else if (result == JFileChooser.APPROVE_OPTION) {
	        	if (chooser.getSelectedFile().exists() && chooser.getSelectedFile().isDirectory()) {
	        		
	        		JOptionPane op = new JOptionPane("Name schon vorhanden!" ,JOptionPane.WARNING_MESSAGE);
	        		JDialog dialog = op.createDialog("Eingabe ung\u00fcltig");
	        		dialog.setAlwaysOnTop(true); //<-- this line
	        		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
	        		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	        		dialog.setVisible(true);
	        		
	        	} else {
	        		
		            // TODO check that your inside rootDir else issue a warning
	        		Path oldFilePath = rootDir.toPath().relativize(selFile.toPath());
		            
		            File newFile = chooser.getSelectedFile();
		            Path  newFilePath = rootDir.toPath().relativize(newFile.toPath());
		            
		            System.out.println("alter Pfad:" + oldFilePath.toString());
		            System.out.println("neuer Pfad:" + newFilePath.toString());

		            // cut away root directory and run renameFolder()
					RENAMEMAP.renamePath(newFilePath ,oldFilePath);
		            
					// try to rename (move) file
		            try {
						Files.move(selFile.toPath(), newFile.toPath());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
		            
		            // renew the selected File after renaming (moving) it
		            if(chooser.getSelectedFile() != null && (chooser.getSelectedFile().exists() || chooser.getSelectedFile().isDirectory())) {
                		selFile = chooser.getSelectedFile();
		            }
	        	}
	        }
        }
        System.out.println("Abbruch");
        chooser.setVisible(false);
        chooser.removeAll();

	}

	/**
	 * Help -> About.
	 */
	@FXML
	public void handleAbout() {
		getMain().setView(Main.ABOUT);
	}
}
