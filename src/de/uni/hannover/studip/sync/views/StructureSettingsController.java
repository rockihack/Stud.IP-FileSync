package de.uni.hannover.studip.sync.views;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.utils.SimpleAlert;

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
	private RadioButton option1;

	@FXML
	private RadioButton option2;

	@FXML
	private RadioButton option3;

	@FXML
	private RadioButton option4;

	@FXML
	private TextField structureField;

	/**
	 * The initialize method is automatically invoked by the FXMLLoader.
	 */
	@FXML
	public void initialize() {
		final String folderStructure = CONFIG.getFolderStructure();

		option1.setTooltip(new Tooltip("z.B. SoSe 2015/Analysis A/vorlesung und SoSe 2015/Analysis A/uebung"));
		option2.setTooltip(new Tooltip("z.B. SoSe 2015/Analysis A und SoSe 2015/Übung: Analysis A"));
		option3.setTooltip(new Tooltip("z.B. Analysis A/15ss/vorlesung und Analysis A/15ss/uebung"));
		structureField.setTooltip(new Tooltip("Erlaubt sind :semester, :sem, :course, :lecture, :type"));
		structureField.setText(folderStructure);

		switch(folderStructure) {
		case ":semester/:lecture/:type":
			structureGroup.selectToggle(option1);
			break;
		case ":semester/:course":
			structureGroup.selectToggle(option2);
			break;
		case ":lecture/:sem/:type":
			structureGroup.selectToggle(option3);
			break;
		default:
			structureGroup.selectToggle(option4);
			break;
		}

		structureGroup.selectedToggleProperty().addListener(
			(observableValue, oldValue, newValue) -> {
				handleSave();
			});
	}

	@FXML
	public void handleSave() {
		final RadioButton selected = (RadioButton) structureGroup.getSelectedToggle();

		try {
			if (option1.equals(selected)) {
				CONFIG.setFolderStructure(":semester/:lecture/:type");
				structureField.setText(":semester/:lecture/:type");
			} else if (option2.equals(selected)) {
				CONFIG.setFolderStructure(":semester/:course");
				structureField.setText(":semester/:course");
			} else if (option3.equals(selected)) {
				CONFIG.setFolderStructure(":lecture/:sem/:type");
				structureField.setText(":lecture/:sem/:type");
			} else {
				// FIXME Validate input.
				CONFIG.setFolderStructure(structureField.getText());
			}

		} catch (IOException e) {
			SimpleAlert.exception(e);
		}
	}
}
