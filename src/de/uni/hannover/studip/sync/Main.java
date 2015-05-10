package de.uni.hannover.studip.sync;
	
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.LinkedList;

import de.uni.hannover.studip.sync.views.AbstractController;
import de.uni.hannover.studip.sync.views.RootLayoutController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class Main extends Application {

	// App name (titlebar).
	public static final String APP_NAME = "Stud.IP FileSync **Beta**";

	// Views.
	public static final String OVERVIEW = "Overview";
	public static final String NEW_DOCUMENTS = "NewDocuments";
	public static final String OAUTH = "OAuth";
	public static final String OAUTH_WEBVIEW = "OAuthWebview";
	public static final String OAUTH_COMPLETE = "OAuthComplete";
	public static final String SETTINGS = "Settings";
	public static final String SYNC_SETTINGS = "SyncSettings";
	public static final String ABOUT = "About";

	private static final LinkedList<String> VIEW_HISTORY = new LinkedList<String>();

	private Stage primaryStage;
	private BorderPane rootLayout;
	private RootLayoutController rootLayoutController;
	private AbstractController currentController;

	@SuppressWarnings("unused")
	private ServerSocket globalAppMutex;

	/**
	 * 
	 */
	@Override
	public void start(final Stage primaryStage) {
		this.primaryStage = primaryStage;

		try {
			// Acquire system wide app mutex to allow only one running instance.
			globalAppMutex = new ServerSocket(9001, 10, InetAddress.getLoopbackAddress());

			initRootLayout();

			setView(OVERVIEW);

		} catch (IOException e) {
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Fehler");
			alert.setHeaderText(null);
			alert.setContentText("FileSync läuft bereits.");
			alert.showAndWait();
			Platform.exit();
		}
	}

	private void initRootLayout() {
		try {
			final FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			rootLayoutController = loader.getController();
			rootLayoutController.setMain(this);

			// Init primary stage.
			primaryStage.setScene(new Scene(rootLayout));
			primaryStage.getIcons().add(new Image(Main.class.getResourceAsStream("icon.png")));
			primaryStage.setTitle(APP_NAME);
			//primaryStage.setMinWidth(640);
			//primaryStage.setMinHeight(480);

			primaryStage.setOnCloseRequest(event -> {
				Platform.exit();

				// Terminate worker threads.
				// TODO: More graceful
				System.exit(0);
			});

			primaryStage.show();

		} catch (IOException e) {
			// RootLayout fxml file not found!
			throw new IllegalStateException(e);
		}
	}

	/**
	 * 
	 * @param fxml
	 */
	public void setView(final String fxml) {
		synchronized (VIEW_HISTORY) {
			if (fxml.equals(VIEW_HISTORY.peek())) {
				// Same as the current view.
				return;
			}

			try {
				final FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/" + fxml + ".fxml"));
				rootLayout.setCenter((AnchorPane) loader.load());

				currentController = loader.getController();
				currentController.setMain(this);

				// Push view.
				VIEW_HISTORY.push(fxml);

			} catch (IOException e) {
				// View fxml file not found!
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * 
	 */
	public void setPrevView() {
		synchronized (VIEW_HISTORY) {
			if (VIEW_HISTORY.size() >= 2) {
				// Pop current view.
				VIEW_HISTORY.pop();

				// Set previous view.
				setView(VIEW_HISTORY.pop());
			}
		}
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		launch(args);
	}

	/**
	 * 
	 * @return
	 */
	public Stage getPrimaryStage() {
		return primaryStage;
	}

	/**
	 * 
	 * @return
	 */
	public BorderPane getRootLayout() {
		return rootLayout;
	}

	/**
	 * 
	 * @return
	 */
	public RootLayoutController getRootLayoutController() {
		return rootLayoutController;
	}

	/**
	 * 
	 * @return
	 */
	public AbstractController getController() {
		return currentController;
	}

}
