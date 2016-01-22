package de.uni.hannover.studip.sync.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Alert.AlertType;

/**
 * Alert helper class.
 * 
 * @author Lennart Glauer
 *
 */
public final class SimpleAlert {

	private SimpleAlert() {
		// Utility class.
	}

	public static Optional<ButtonType> alert(final AlertType type, final String title, final String header, final String content) {
		final Alert alert = new Alert(type);
		alert.setTitle(title);
		alert.setHeaderText(header);
		alert.setContentText(content);
		alert.setResizable(true);
		return alert.showAndWait();
	}

	public static void exception(final Throwable t) {
		final StringWriter writer = new StringWriter();
		t.printStackTrace(new PrintWriter(writer));
		alert(AlertType.ERROR, "Exception", null, writer.toString());
	}

	public static void error(final String content) {
		alert(AlertType.ERROR, "Fehler", null, content);
	}

	public static void warning(final String content) {
		alert(AlertType.WARNING, "Warnung", null, content);
	}

	public static void info(final String content) {
		alert(AlertType.INFORMATION, "Info", null, content);
	}

	public static ButtonType confirm(final String content) {
		final Optional<ButtonType> result = alert(AlertType.CONFIRMATION, "Bestätigen", null, content);
		return result.isPresent() ? result.get() : ButtonType.CLOSE;
	}
}
