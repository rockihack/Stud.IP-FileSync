package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentFolderTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Alert.AlertType;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class SyncSettingsController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();

	@FXML
	private ChoiceBox<String> overwriteChoicebox;

	@FXML
	private ChoiceBox<String> downloadAllSemestersChoicebox;

	@FXML
	protected ChoiceBox<String> replaceWhitespacesChoicebox;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		overwriteChoicebox.getSelectionModel().select(CONFIG.isOverwriteFiles() ? 0 : 1);
		downloadAllSemestersChoicebox.getSelectionModel().select(CONFIG.isDownloadAllSemesters() ? 0 : 1);
		replaceWhitespacesChoicebox.getSelectionModel().select(CONFIG.getReplaceWhitespaces());

		overwriteChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						CONFIG.setOverwriteFiles(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

		downloadAllSemestersChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						CONFIG.setDownloadAllSemesters(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

		replaceWhitespacesChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (CONFIG.getReplaceWhitespaces() != newValue.intValue()) {
					try {
						handleRenameDocuments(CONFIG.getReplaceWhitespaces(), newValue.intValue());

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});
	}

	/**
	 * 
	 * @param oldValue
	 * @param newValue
	 * @throws IOException
	 */
	private synchronized void handleRenameDocuments(final int oldValue, final int newValue) throws IOException {
		final Alert confirm = new Alert(AlertType.CONFIRMATION);
		confirm.setTitle("Bestätigen");
		confirm.setHeaderText(null);
		confirm.setContentText("Alle Dokumente werden entsprechend umbenannt.\nMöchten Sie wirklich fortfahren?");
		final Optional<ButtonType> result = confirm.showAndWait();

		if (result.get() == ButtonType.OK) {
			try {
				renameDocuments(oldValue, newValue);
				CONFIG.setReplaceWhitespaces(newValue);

			} catch (IOException e) {
				// Rollback.
				replaceWhitespacesChoicebox.getSelectionModel().select(oldValue);
				renameDocuments(newValue, oldValue);

				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Es konnten nicht alle Dokumente umbenannt werden.\nDie Änderungen wurden rückgängig gemacht.");
				alert.showAndWait();
			}

		} else {
			replaceWhitespacesChoicebox.getSelectionModel().select(oldValue);
		}
	}

	/**
	 * 
	 * @param oldValue
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	private static synchronized void renameDocuments(final int oldValue, final int newValue) throws IOException {
		/* Read existing tree. */
		final File treeFile = Config.openTreeFile();
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(treeFile, SemestersTreeNode.class);

		final File rootDirectory = new File(CONFIG.getRootDirectory());

		// Rename documents.
		for (SemesterTreeNode semester : rootNode.semesters) {
			final File oldSemesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title, oldValue));
			if (!oldSemesterDirectory.exists()) {
				continue;
			}

			final File newSemesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title, newValue));
			if(!oldSemesterDirectory.renameTo(newSemesterDirectory)) {
				throw new IOException("Ordner konnte nicht umbenannt werden.\n" + oldSemesterDirectory.getAbsolutePath());
			}

			for (CourseTreeNode course : semester.courses) {
				final File oldCourseDirectory = new File(newSemesterDirectory, FileBrowser.removeIllegalCharacters(course.title, oldValue));
				if (!oldCourseDirectory.exists()) {
					continue;
				}

				final File newCourseDirectory = new File(newSemesterDirectory, FileBrowser.removeIllegalCharacters(course.title, newValue));
				if (!oldCourseDirectory.renameTo(newCourseDirectory)) {
					throw new IOException("Ordner konnte nicht umbenannt werden.\n" + oldCourseDirectory.getAbsolutePath());
				}

				doFolder(course.root, newCourseDirectory, oldValue, newValue);
			}
		}
	}

	/**
	 * 
	 * @param folderNode
	 * @param parentDirectory
	 * @param oldValue
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	private synchronized static void doFolder(final DocumentFolderTreeNode folderNode, final File parentDirectory, final int oldValue, final int newValue) throws IOException {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			final File oldFolderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name, oldValue));
			if (!oldFolderDirectory.exists()) {
				continue;
			}

			final File newFolderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name, newValue));
			if (!oldFolderDirectory.renameTo(newFolderDirectory)) {
				throw new IOException("Ordner konnte nicht umbenannt werden.\n" + oldFolderDirectory.getAbsolutePath());
			}

			doFolder(folder, newFolderDirectory, oldValue, newValue);
		}

		for (DocumentTreeNode document : folderNode.documents) {
			final String oldFileName = FileBrowser.removeIllegalCharacters(document.fileName, oldValue);
			final String newFileName = FileBrowser.removeIllegalCharacters(document.fileName, newValue);
			File oldFile, newFile;

			oldFile = new File(parentDirectory, oldFileName);
			if (oldFile.exists()) {
				newFile = new File(parentDirectory, newFileName);
				if (!oldFile.renameTo(newFile)) {
					throw new IOException("Datei konnte nicht umbenannt werden.\n" + oldFile.getAbsolutePath());
				}
			}

			for (int i = 1; ; i++) {
				oldFile = new File(parentDirectory, FileBrowser.appendFilename(oldFileName, "_v" + i));
				if (!oldFile.exists()) {
					break;
				}

				newFile = new File(parentDirectory, FileBrowser.appendFilename(newFileName, "_v" + i));
				if (!oldFile.renameTo(newFile)) {
					throw new IOException("Datei konnte nicht umbenannt werden.\n" + oldFile.getAbsolutePath());
				}
			}
		}
	}
}
