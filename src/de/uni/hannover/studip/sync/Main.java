package de.uni.hannover.studip.sync;
	
import java.io.IOException;
import java.util.LinkedList;

import de.uni.hannover.studip.sync.views.AbstractController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;


public class Main extends Application {

	// App name (titlebar).
	private static final String APP_NAME = "Stud.IP FileSync **Beta**";

	// Views.
	public static final String OVERVIEW = "Overview";
	public static final String OAUTH = "OAuth";
	public static final String OAUTH_WEBVIEW = "OAuthWebview";
	public static final String OAUTH_COMPLETE = "OAuthComplete";
	public static final String SETTINGS = "Settings";
	public static final String ABOUT = "About";

	private LinkedList<String> viewHistory = new LinkedList<String>();

	private Stage primaryStage;
	private BorderPane rootLayout;

	@Override
	public void start(Stage primaryStage) {
		this.primaryStage = primaryStage;

		initRootLayout();

		setView(OVERVIEW);
	}

	private void initRootLayout() {
		try {
			FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/RootLayout.fxml"));
			this.rootLayout = (BorderPane) loader.load();

			AbstractController controller = loader.getController();
			controller.setMain(this);

			// Init primary stage.
			primaryStage.setScene(new Scene(this.rootLayout));
			primaryStage.setTitle(APP_NAME);
			primaryStage.setMinWidth(640);
			primaryStage.setMinHeight(480);
			primaryStage.show();

		} catch (IOException e) {
			// RootLayout fxml file not found!
			throw new IllegalStateException(e);
		}
	}

	public void setView(String fxml) {
		synchronized (viewHistory) {
			if (viewHistory.peek() == fxml) {
				// Same as the current view.
				return;
			}

			try {
				FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/" + fxml + ".fxml"));
				rootLayout.setCenter((AnchorPane) loader.load());

				AbstractController controller = loader.getController();
				controller.setMain(this);

				// Push view.
				viewHistory.push(fxml);

			} catch (IOException e) {
				// View fxml file not found!
				throw new IllegalStateException(e);
			}
		}
	}

	public void setPrevView() {
		synchronized (viewHistory) {
			if (viewHistory.size() >= 2) {
				// Pop current view.
				viewHistory.pop();

				// Set previous view.
				setView(viewHistory.pop());
			}
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public BorderPane getRootLayout() {
		return rootLayout;
	}

}
