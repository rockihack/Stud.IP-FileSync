package de.uni.hannover.studip.sync.views;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.PathBuilder;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileBrowser;
import de.uni.hannover.studip.sync.utils.SimpleAlert;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class NewDocumentsController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();
	private static final ObjectMapper MAPPER = Config.getMapper();

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
		final String rootDir = CONFIG.getRootDirectory();
		if (rootDir == null || rootDir.isEmpty()) {
			SimpleAlert.error("Kein Ziel Ordner gewählt.");
			return;
		}

		if (!Main.TREE_LOCK.tryLock()) {
			return;
		}

		try {
			final Path rootDirectory = Paths.get(rootDir);
			final String folderStructure = CONFIG.getFolderStructure();

			/* Read existing tree. */
			final SemestersTreeNode rootNode = MAPPER.readerFor(SemestersTreeNode.class)
					.readValue(Files.newInputStream(Config.openTreeFile()));

			/* Build list of documents. */
			for (final SemesterTreeNode semester : rootNode.semesters) {
				for (final CourseTreeNode course : semester.courses) {
					doFolder(semester, course, course.root, PathBuilder.toPath(folderStructure, rootDirectory, semester, course));
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
			tableView.setRowFactory(callback -> new NewDocumentsTableRow());

			// Set list items.
			tableView.setItems(documentList);

			// Sort columns accordingly to the document chdate.
			tableView.getSortOrder().add(dateColumn);

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
			SimpleAlert.exception(e);

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
		for (final DocumentFolderTreeNode folder : folderNode.folders) {
			doFolder(semesterNode, courseNode, folder,
					StudIPApiProvider.DEFAULT_FOLDER.equals(folder.name.trim())
					? parentDirectory
					: parentDirectory.resolve(FileBrowser.removeIllegalCharacters(folder.name)));
		}

		for (final DocumentTreeNode document : folderNode.documents) {
			documentList.add(new NewDocumentsModel(semesterNode, courseNode, document, parentDirectory.resolve(FileBrowser.removeIllegalCharacters(document.fileName))));
		}
	}

	/**
	 * Table row model.
	 */
	private static class NewDocumentsTableRow extends TableRow<NewDocumentsModel> {
		public NewDocumentsTableRow() {
			super();

			// Click listener.
			setOnMouseClicked(event -> {
				final NewDocumentsModel selectedItem = getItem();
				if (selectedItem != null && event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
					try {
						FileBrowser.open(selectedItem.getDocumentFile());

					} catch (IOException e) {
						SimpleAlert.exception(e);
					}
				}
			});

			final Tooltip tooltip = new Tooltip();
			tooltip.setWrapText(true);
			tooltip.setMaxWidth(600);
			setTooltip(tooltip);
		}

		@Override
		public void updateItem(final NewDocumentsModel item, final boolean empty) {
			super.updateItem(item, empty);

			if (!empty && item != null) {
				getTooltip().setText(item.getDocumentDescription());
			}
		}
	}
}
