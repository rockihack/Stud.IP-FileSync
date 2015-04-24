package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;
import javafx.fxml.FXML;

public class AboutController extends AbstractController {
	
	@FXML
	public void handlePrev() {
		// TODO
		getMain().setView(Main.OAUTH);
	}

}
