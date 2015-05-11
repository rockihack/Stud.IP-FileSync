package de.uni.hannover.studip.sync.views;

import javafx.fxml.FXML;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class HelpController extends AbstractController {

	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
