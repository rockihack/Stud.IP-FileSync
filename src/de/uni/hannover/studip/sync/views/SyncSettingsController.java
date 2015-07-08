package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

			} catch (NoSuchFileException | JsonParseException | JsonMappingException e) {
				// Tree file is invalid or does not exist yet.
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
		final String rooDir = CONFIG.getRootDirectory();
		if (oldValue == newValue || rooDir == null || rooDir.isEmpty()) {
			return;
		}

		final Path rootDirectory = Paths.get(rooDir);

		/* Read existing tree. */
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(Files.newBufferedReader(Config.openTreeFile()), SemestersTreeNode.class);

		/* Rename documents. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			final Path oldSemesterDirectory = rootDirectory.resolve(FileBrowser.removeIllegalCharacters(semester.title, oldValue));
			if (!Files.exists(oldSemesterDirectory)) {
				continue;
			}

			final Path newSemesterDirectory = rootDirectory.resolve(FileBrowser.removeIllegalCharacters(semester.title, newValue));
			Files.move(oldSemesterDirectory, newSemesterDirectory, StandardCopyOption.REPLACE_EXISTING);

			for (CourseTreeNode course : semester.courses) {
				final Path oldCourseDirectory = newSemesterDirectory.resolve(FileBrowser.removeIllegalCharacters(course.title, oldValue));
				if (!Files.exists(oldCourseDirectory)) {
					continue;
				}

				final Path newCourseDirectory = newSemesterDirectory.resolve(FileBrowser.removeIllegalCharacters(course.title, newValue));
				Files.move(oldCourseDirectory, newCourseDirectory, StandardCopyOption.REPLACE_EXISTING);

				doFolder(course.root, newCourseDirectory, oldValue, newValue);
			}
		}
	}

	/**
	 * Traverse folder structure (recursive).
	 * 
	 * @param folderNode
	 * @param parentDirectory
	 * @param oldValue
	 * @param newValue
	 * @return
	 * @throws IOException 
	 */
	private synchronized static void doFolder(final DocumentFolderTreeNode folderNode, final Path parentDirectory, final int oldValue, final int newValue) throws IOException {
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			final Path oldFolderDirectory = parentDirectory.resolve(FileBrowser.removeIllegalCharacters(folder.name, oldValue));
			if (!Files.exists(oldFolderDirectory)) {
				continue;
			}

			final Path newFolderDirectory = parentDirectory.resolve(FileBrowser.removeIllegalCharacters(folder.name, newValue));
			Files.move(oldFolderDirectory, newFolderDirectory, StandardCopyOption.REPLACE_EXISTING);

			doFolder(folder, newFolderDirectory, oldValue, newValue);
		}

		for (DocumentTreeNode document : folderNode.documents) {
			final String oldFileName = FileBrowser.removeIllegalCharacters(document.fileName, oldValue);
			final String newFileName = FileBrowser.removeIllegalCharacters(document.fileName, newValue);
			Path oldFile, newFile;

			oldFile = parentDirectory.resolve(oldFileName);
			if (Files.exists(oldFile)) {
				newFile = parentDirectory.resolve(newFileName);
				Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
			}

			for (int i = 1; ; i++) {
				oldFile = parentDirectory.resolve(FileBrowser.appendFilename(oldFileName, "_v" + i));
				if (!Files.exists(oldFile)) {
					break;
				}

				newFile = parentDirectory.resolve(FileBrowser.appendFilename(newFileName, "_v" + i));
				Files.move(oldFile, newFile, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
