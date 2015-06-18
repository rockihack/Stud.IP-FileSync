package de.uni.hannover.studip.sync.views;

import javafx.fxml.FXML;
import de.uni.hannover.studip.sync.Main;

/**
 * 
 * @author Lennart Glauer
 *
 */
public abstract class AbstractController {

	private Main main;

	public Main getMain() {
		return main;
	}

	public void setMain(final Main main) {
		this.main = main;
	}

	@FXML
	public void handlePrev() {
		main.setPrevView();
	}
}
