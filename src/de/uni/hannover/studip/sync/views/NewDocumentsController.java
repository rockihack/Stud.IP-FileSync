package de.uni.hannover.studip.sync.views;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentFolderTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentTreeNode;
import de.uni.hannover.studip.sync.datamodel.NewDocumentsModel;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

public class NewDocumentsController extends AbstractController {

	private ObservableList<NewDocumentsModel> documentList = FXCollections.observableArrayList();

	@FXML
	private TableView<NewDocumentsModel> tableView;

	@FXML
	private TableColumn<NewDocumentsModel, Date> dateColumn;

	@FXML
	private TableColumn<NewDocumentsModel, String> documentColumn;

	@FXML
	private TableColumn<NewDocumentsModel, String> courseColumn;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		try {
			/* Read existing tree. */
			File treeFile = Config.openTreeFile();
			ObjectMapper mapper = new ObjectMapper();
			SemestersTreeNode rootNode = mapper.readValue(treeFile, SemestersTreeNode.class);

			File rootDirectory = new File(Config.getInstance().getRootDirectory());

			// Build list of documents.
			for (SemesterTreeNode semester : rootNode.semesters) {
				File semesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title));

				for (CourseTreeNode course : semester.courses) {
					File courseDirectory = new File(semesterDirectory, FileBrowser.removeIllegalCharacters(course.title));
					doFolder(course, course.root, courseDirectory);
				}
			}

			// Init table properties.
			dateColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, Date>("documentChdate"));
			documentColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("documentName"));
			courseColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("courseTitle"));

			// Auto resize.
			dateColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));
			documentColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.45));
			courseColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.45));

			// Click listener.
			tableView.setRowFactory(new Callback<TableView<NewDocumentsModel>, TableRow<NewDocumentsModel>>() {
				@Override
				public TableRow<NewDocumentsModel> call(TableView<NewDocumentsModel> arg0) {
					TableRow<NewDocumentsModel> row = new TableRow<NewDocumentsModel>();

					row.setOnMouseClicked(new EventHandler<MouseEvent>() {
						@Override
						public void handle(MouseEvent event) {
							NewDocumentsModel selectedItem = tableView.getSelectionModel().getSelectedItem();

							if (selectedItem != null && event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
								// TODO: Add fail message box.
								FileBrowser.open(selectedItem.getDocumentFile());
							}
						}
					});

					return row;
				}
			});

			tableView.setItems(documentList);

			// Sort columns accordingly to the document chdate.
			tableView.getSortOrder().add(dateColumn);

		} catch (IOException e) {
			// TODO: Add fail message box.
			throw new IllegalStateException(e);
		}
	}

	private void doFolder(CourseTreeNode courseNode, DocumentFolderTreeNode folderNode, File parentDirectory) {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			File folderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name));
			doFolder(courseNode, folder, folderDirectory);
		}

		for (DocumentTreeNode document : folderNode.documents) {
			File documentFile = new File(parentDirectory, FileBrowser.removeIllegalCharacters(document.filename));
			documentList.add(new NewDocumentsModel(courseNode, document, documentFile));
		}
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
