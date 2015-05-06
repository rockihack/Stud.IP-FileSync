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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

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
			File treeFile = Config.getInstance().openTreeFile();
			ObjectMapper mapper = new ObjectMapper();
			SemestersTreeNode rootNode = mapper.readValue(treeFile, SemestersTreeNode.class);

			// Build list of documents.
			for (SemesterTreeNode semester : rootNode.semesters) {
				for (CourseTreeNode course : semester.courses) {
					doFolder(course, course.root);
				}
			}

			// Init table.
			dateColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, Date>("documentChdate"));
			documentColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("documentName"));
			courseColumn.setCellValueFactory(new PropertyValueFactory<NewDocumentsModel, String>("courseTitle"));

			// Auto resize.
			dateColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.1));
			documentColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.45));
			courseColumn.prefWidthProperty().bind(tableView.widthProperty().multiply(0.45));

			tableView.setItems(documentList);

			// Sort columns accordingly to the document chdate.
			tableView.getSortOrder().add(dateColumn);

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private void doFolder(CourseTreeNode courseNode, DocumentFolderTreeNode folderNode) {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			doFolder(courseNode, folder);
		}

		for (DocumentTreeNode document : folderNode.documents) {
			documentList.add(new NewDocumentsModel(courseNode, document));
		}
	}

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
