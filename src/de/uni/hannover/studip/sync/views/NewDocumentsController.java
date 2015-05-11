package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentFolderTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentTreeNode;
import de.uni.hannover.studip.sync.datamodel.NewDocumentsModel;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert.AlertType;
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
		try {
			/* Read existing tree. */
			final File treeFile = Config.openTreeFile();
			final ObjectMapper mapper = new ObjectMapper();
			final SemestersTreeNode rootNode = mapper.readValue(treeFile, SemestersTreeNode.class);

			final File rootDirectory = new File(Config.getInstance().getRootDirectory());

			// Build list of documents.
			for (SemesterTreeNode semester : rootNode.semesters) {
				final File semesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title));

				for (CourseTreeNode course : semester.courses) {
					final File courseDirectory = new File(semesterDirectory, FileBrowser.removeIllegalCharacters(course.title));
					doFolder(semester, course, course.root, courseDirectory);
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

			// Click listener.
			tableView.setRowFactory(callback -> {
				final TableRow<NewDocumentsModel> row = new TableRow<NewDocumentsModel>();

				row.setOnMouseClicked(event -> {
					final NewDocumentsModel selectedItem = tableView.getSelectionModel().getSelectedItem();

					if (selectedItem != null && event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
						FileBrowser.open(selectedItem.getDocumentFile());
					}
				});

				return row;
			});

			tableView.setItems(documentList);

			// Sort columns accordingly to the document chdate.
			tableView.getSortOrder().add(dateColumn);

		} catch (JsonParseException | JsonMappingException e) {
			Platform.runLater(() -> {
				final Alert alert = new Alert(AlertType.INFORMATION);
				alert.setTitle("Info");
				alert.setHeaderText(null);
				alert.setContentText("Keine Dokumente gefunden.\nSie müssen zuerst Ihre Dokumente synchronisieren.");
				alert.showAndWait();

				// Redirect to overview.
				getMain().setView(Main.OVERVIEW);

				// Start the sync.
				final OverviewController overview = (OverviewController) getMain().getController();
				overview.handleSync();
			});

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void doFolder(final SemesterTreeNode semesterNode, final CourseTreeNode courseNode, final DocumentFolderTreeNode folderNode, final File parentDirectory) {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			final File folderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name));
			doFolder(semesterNode, courseNode, folder, folderDirectory);
		}

		for (DocumentTreeNode document : folderNode.documents) {
			final File documentFile = new File(parentDirectory, FileBrowser.removeIllegalCharacters(document.filename));
			documentList.add(new NewDocumentsModel(semesterNode, courseNode, document, documentFile));
		}
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
