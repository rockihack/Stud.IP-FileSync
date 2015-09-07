package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.PathBuilder;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class NewDocumentsController extends AbstractController {

	private final ObservableList<NewDocumentsModel> documentList = FXCollections.observableArrayList();

	@FXML
	protected TableView<NewDocumentsModel> tableView;

	@FXML
	private TableColumn<NewDocumentsModel, Date> dateColumn;

	@FXML
	private TableColumn<NewDocumentsModel, String> documentColumn;

	@FXML
	private TableColumn<NewDocumentsModel, String> courseColumn;

	@FXML
	private TableColumn<NewDocumentsModel, String> semesterColumn;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		if (!Main.TREE_LOCK.tryLock()) {
			return;
		}

		try {
			final String rootDir = Config.getInstance().getRootDirectory();
			if (rootDir == null || rootDir.isEmpty()) {
				final Alert alert = new Alert(AlertType.ERROR);
				alert.setTitle("Fehler");
				alert.setHeaderText(null);
				alert.setContentText("Kein Ziel Ordner gewählt.");
				alert.showAndWait();
				return;
			}

			final Path rootDirectory = Paths.get(rootDir);

			/* Read existing tree. */
			final ObjectMapper mapper = new ObjectMapper();
			final SemestersTreeNode rootNode = mapper.readValue(Files.newBufferedReader(Config.openTreeFile()), SemestersTreeNode.class);

			final String folderStructure = Config.getInstance().getFolderStructure();

			// Build list of documents.
			for (SemesterTreeNode semester : rootNode.semesters) {
				for (CourseTreeNode course : semester.courses) {
					doFolder(semester, course, course.root, new PathBuilder(folderStructure, rootDirectory, semester, course).toPath());
				}
			}

			// Init table properties.
			dateColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, Date>("documentChdate"));
			documentColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("documentName"));
			courseColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("courseTitle"));
			semesterColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("semesterTitle"));

			// Auto resize.
			dateColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));
			documentColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.4));
			courseColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.4));
			semesterColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));

			// Table row factory.
			tableView.setRowFactory(callback -> {
				return new TableRow<NewDocumentsModel>() {
					// Init.
					{
						// Click listener.
						setOnMouseClicked(event -> {
							final NewDocumentsModel selectedItem = getItem();

							if (selectedItem != null && event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
								final Path selectedFile = selectedItem.getDocumentFile();

								try {
									FileBrowser.open(selectedFile);

								} catch (IOException e) {
									final Alert alert = new Alert(AlertType.ERROR);
									alert.setTitle("Fehler");
									alert.setHeaderText(null);
									alert.setContentText("Datei wurde nicht gefunden.\n" + selectedFile.toAbsolutePath());
									alert.showAndWait();
								}
							}
						});

						// Tooltip.
						final Tooltip tooltip = new Tooltip();
						tooltip.setWrapText(true);
						tooltip.setMaxWidth(600);
						setTooltip(tooltip);
					}

					@Override
					public void updateItem(final NewDocumentsModel item, final boolean empty) {
						super.updateItem(item, empty);

						if (item != null) {
							getTooltip().setText(item.getDocumentDescription());
						}
					}
				};
			});

			// Set list items.
			tableView.setItems(documentList);

			// Sort columns accordingly to the document chdate.
			tableView.getSortOrder().add(dateColumn);

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
			throw new IllegalStateException(e);

		} finally {
			Main.TREE_LOCK.unlock();
		}
	}

	/**
	 * Traverse folder structure (recursive).
	 * 
	 * @param semesterNode
	 * @param courseNode
	 * @param folderNode
	 * @param parentDirectory
	 */
	private void doFolder(final SemesterTreeNode semesterNode, final CourseTreeNode courseNode, final DocumentFolderTreeNode folderNode, final Path parentDirectory) {
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			doFolder(semesterNode, courseNode, folder,
					StudIPApiProvider.DEFAULT_FOLDER.equals(folder.name.trim())
					? parentDirectory
					: parentDirectory.resolve(FileBrowser.removeIllegalCharacters(folder.name)));
		}

		for (DocumentTreeNode document : folderNode.documents) {
			documentList.add(new NewDocumentsModel(semesterNode, courseNode, document, parentDirectory.resolve(FileBrowser.removeIllegalCharacters(document.fileName))));
		}
	}
}
