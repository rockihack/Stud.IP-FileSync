package de.uni.hannover.studip.sync.views;

import javafx.fxml.FXML;

public class AboutController extends AbstractController {
	
	@FXML
	public void handlePrev() {
		getMain().setPrevView();
	}

}
