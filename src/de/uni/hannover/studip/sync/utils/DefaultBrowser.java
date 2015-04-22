package de.uni.hannover.studip.sync.utils;

import java.awt.Desktop;
import java.net.URI;

import javax.swing.JOptionPane;

public class DefaultBrowser {
	
	private static final String DIALOG_TITLE = "Bitte öffnen Sie den folgenden Link in Ihrem Browser.";
	private static final String DIALOG_MESSAGE = "Bitte öffnen Sie den folgenden Link in Ihrem Browser.";

	/**
	 * Open the uri in the users default browser.
	 * 
	 * @param uri
	 */
	public static void open(String uri) {
		try{
			Desktop.getDesktop().browse(new URI(uri));
			
		} catch (Exception e) {
			JOptionPane.showInputDialog(null, DIALOG_MESSAGE, DIALOG_TITLE,
					JOptionPane.INFORMATION_MESSAGE, null, null, uri);
		}
	}
	
}
