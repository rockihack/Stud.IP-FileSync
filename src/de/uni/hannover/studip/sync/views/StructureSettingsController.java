package de.uni.hannover.studip.sync.views;

import java.io.IOException;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import de.uni.hannover.studip.sync.models.Config;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class StructureSettingsController extends AbstractController {

	private static final Config CONFIG = Config.getInstance();

	@FXML
	private ToggleGroup structureGroup;

	@FXML
	private TextField structureField;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		final ObservableList<Toggle> toggles = structureGroup.getToggles();
		final String folderStructure = CONFIG.getFolderStructure();

		structureField.setText(folderStructure);

		switch(folderStructure) {
		case ":semester/:course":
			structureGroup.selectToggle(toggles.get(0));
			break;
		case ":semester/:lecture/:type":
			structureGroup.selectToggle(toggles.get(1));
			break;
		case ":lecture/:sem/:type":
			structureGroup.selectToggle(toggles.get(2));
			break;
		default:
			structureGroup.selectToggle(toggles.get(3));
			break;
		}

		structureGroup.selectedToggleProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				handleSave();
			});
	}

	@FXML
	public void handleSave() {
		final String radio = ((RadioButton) structureGroup.getSelectedToggle()).getText();

		try {
			switch(radio) {
			case ":semester/:course":
			case ":semester/:lecture/:type":
			case ":lecture/:sem/:type":
				structureField.setText(radio);
				CONFIG.setFolderStructure(radio);
				break;
			default:
				CONFIG.setFolderStructure(structureField.getText());
				break;
			}

		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
