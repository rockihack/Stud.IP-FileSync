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
		final Config config = Config.getInstance();

		overwriteChoicebox.getSelectionModel().select(config.isOverwriteFiles() ? 0 : 1);
		downloadAllSemestersChoicebox.getSelectionModel().select(config.isDownloadAllSemesters() ? 0 : 1);
		replaceWhitespacesChoicebox.getSelectionModel().select(config.getReplaceWhitespaces());

		overwriteChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						config.setOverwriteFiles(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

		downloadAllSemestersChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (!oldValue.equals(newValue)) {
					try {
						config.setDownloadAllSemesters(newValue.intValue() == 0);

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});

		replaceWhitespacesChoicebox.getSelectionModel().selectedIndexProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				if (config.getReplaceWhitespaces() != newValue.intValue()) {
					try {
						final Alert confirm = new Alert(AlertType.CONFIRMATION);
						confirm.setTitle("Bestätigen");
						confirm.setHeaderText(null);
						confirm.setContentText("Alle Dokumente werden entsprechend umbenannt.\nMöchten Sie wirklich fortfahren?");
						final Optional<ButtonType> result = confirm.showAndWait();

						if (result.get() == ButtonType.OK) {
							if (renameDocuments(config.getReplaceWhitespaces(), newValue.intValue())) {
								// Success.
								config.setReplaceWhitespaces(newValue.intValue());

							} else {
								// Rollback.
								renameDocuments(newValue.intValue(), config.getReplaceWhitespaces());
								replaceWhitespacesChoicebox.getSelectionModel().select(config.getReplaceWhitespaces());

								final Alert alert = new Alert(AlertType.ERROR);
								alert.setTitle("Fehler");
								alert.setHeaderText(null);
								alert.setContentText("Es konnten nicht alle Dokumente umbenannt werden.\nDie Änderungen wurden rückgängig gemacht.");
								alert.showAndWait();
							}

						} else {
							replaceWhitespacesChoicebox.getSelectionModel().select(config.getReplaceWhitespaces());
						}

					} catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			});
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

	/**
	 * 
	 * @param oldValue
	 * @param newValue
	 * @return
	 * @throws IOException
	 */
	private synchronized boolean renameDocuments(final int oldValue, final int newValue) throws IOException {
		/* Read existing tree. */
		final File treeFile = Config.openTreeFile();
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(treeFile, SemestersTreeNode.class);

		final File rootDirectory = new File(Config.getInstance().getRootDirectory());

		// Rename documents.
		for (SemesterTreeNode semester : rootNode.semesters) {
			final File _semesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title, oldValue));
			if (!_semesterDirectory.exists()) {
				continue;
			}

			final File semesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title, newValue));
			if(!_semesterDirectory.renameTo(semesterDirectory)) {
				return false;
			}

			for (CourseTreeNode course : semester.courses) {
				final File _courseDirectory = new File(semesterDirectory, FileBrowser.removeIllegalCharacters(course.title, oldValue));
				if (!_courseDirectory.exists()) {
					continue;
				}

				final File courseDirectory = new File(semesterDirectory, FileBrowser.removeIllegalCharacters(course.title, newValue));
				if (!_courseDirectory.renameTo(courseDirectory) || !doFolder(course.root, courseDirectory, oldValue, newValue)) {
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 
	 * @param folderNode
	 * @param parentDirectory
	 * @param oldValue
	 * @param newValue
	 * @return
	 */
	private boolean doFolder(final DocumentFolderTreeNode folderNode, final File parentDirectory, final int oldValue, final int newValue) {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			final File _folderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name, oldValue));
			if (!_folderDirectory.exists()) {
				continue;
			}

			final File folderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name, newValue));
			if (!_folderDirectory.renameTo(folderDirectory) || !doFolder(folder, folderDirectory, oldValue, newValue)) {
				return false;
			}
		}

		for (DocumentTreeNode document : folderNode.documents) {
			final File _documentFile = new File(parentDirectory, FileBrowser.removeIllegalCharacters(document.filename, oldValue));
			if (!_documentFile.exists()) {
				continue;
			}

			final File documentFile = new File(parentDirectory, FileBrowser.removeIllegalCharacters(document.filename, newValue));
			if (!_documentFile.renameTo(documentFile)) {
				return false;
			}
		}

		return true;
	}
}
