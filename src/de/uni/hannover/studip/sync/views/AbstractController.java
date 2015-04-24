package de.uni.hannover.studip.sync.views;

import de.uni.hannover.studip.sync.Main;

public abstract class AbstractController {
	
	private Main main;
	
	public Main getMain() {
		return this.main;
	}
	
	public void setMain(Main main) {
		this.main = main;
	}

}
