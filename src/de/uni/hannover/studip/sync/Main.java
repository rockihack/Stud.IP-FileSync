package de.uni.hannover.studip.sync;
	
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.uni.hannover.studip.sync.utils.Cli;
import de.uni.hannover.studip.sync.utils.SimpleAlert;
import de.uni.hannover.studip.sync.views.AbstractController;
import de.uni.hannover.studip.sync.views.RootLayoutController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;

/**
 * 
 * @author Lennart Glauer
 *
 */
public class Main extends Application {

	/**
	 * App name (titlebar).
	 */
	public static final String APP_NAME = "Stud.IP FileSync";

	/**
	 * Views.
	 */
	public static final String OVERVIEW = "Overview";
	public static final String NEW_DOCUMENTS = "NewDocuments";
	public static final String OAUTH = "OAuth";
	public static final String OAUTH_WEBVIEW = "OAuthWebview";
	public static final String SETUP_ROOTDIR = "SetupRootDir";
	public static final String SETUP_STRUCTURE = "SetupStructure";
	public static final String SETUP_SYNC = "SetupSync";
	public static final String SETTINGS = "Settings";
	public static final String STRUCTURE_SETTINGS = "StructureSettings";
	public static final String SYNC_SETTINGS = "SyncSettings";
	public static final String HELP = "Help";
	public static final String ABOUT = "About";

	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * Keep track of previous views.
	 */
	private static final LinkedList<String> VIEW_HISTORY = new LinkedList<>();

	/**
	 * Global tree file lock.
	 */
	public static final ReentrantLock TREE_LOCK = new ReentrantLock();

	/**
	 * Global flag to signal graceful shutdown of worker threads on app exit.
	 */
	public static volatile boolean exitPending;

	private Stage primaryStage;
	private BorderPane rootLayout;
	private RootLayoutController rootLayoutController;
	private AbstractController currentController;

	@SuppressWarnings("unused")
	private static ServerSocket globalAppMutex;

	static {
		/*
		 * Default Uncaught Exception Handler.
		 */
		Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
			throwable.printStackTrace();

			// Signal worker threads to terminate gracefully.
			exitPending = true;

			// Show stacktrace and terminate app.
			Platform.runLater(() -> {
				SimpleAlert.exception(throwable);
				Platform.exit();
			});
		});

		/*
		 * Global log level.
		 */
		LOG.setLevel(Level.WARNING);
	}

	/**
	 * Main method.
	 * 
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			// Acquire system wide app mutex to allow only one running instance.
			globalAppMutex = new ServerSocket(9001, 10, InetAddress.getLoopbackAddress());

			Cli.handleArgs(args);
			launch(args);

		} catch (IOException e) {
			System.out.println("FileSync läuft bereits.");
			System.exit(1);
		}
	}

	/**
	 * JavaFX application start method.
	 */
	@Override
	public void start(final Stage primaryStage) {
		this.primaryStage = primaryStage;
		initRootLayout();
		setView(OVERVIEW);
	}

	/**
	 * Init application root layout (menu bar).
	 */
	private void initRootLayout() {
		try {
			final FXMLLoader loader = new FXMLLoader(Main.class.getResource("views/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			rootLayoutController = loader.getController();
			rootLayoutController.setMain(this);

			// Init primary stage.
			primaryStage.setScene(new Scene(rootLayout));
			primaryStage.getIcons().addAll(
					new Image(Main.class.getResourceAsStream("icon_16.png")),
					new Image(Main.class.getResourceAsStream("icon_24.png")),
					new Image(Main.class.getResourceAsStream("icon_32.png")),
					new Image(Main.class.getResourceAsStream("icon_48.png")),
					new Image(Main.class.getResourceAsStream("icon_64.png")),
					new Image(Main.class.getResourceAsStream("icon_96.png")),
					new Image(Main.class.getResourceAsStream("icon_128.png")),
					new Image(Main.class.getResourceAsStream("icon_256.png")),
					new Image(Main.class.getResourceAsStream("icon_512.png")));
			primaryStage.setTitle(APP_NAME);

			primaryStage.setOnCloseRequest(event -> {
				// Signal worker threads to terminate gracefully.
				exitPending = true;
				Platform.exit();
			});

			primaryStage.show();

		} catch (IOException e) {
			// RootLayout fxml file not found!
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Set active view.
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

				VIEW_HISTORY.push(fxml);

			} catch (IOException e) {
				// View fxml file not found!
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * Set previous view.
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
	 * Return primary stage (window).
	 * 
	 * @return
	 */
	public Stage getPrimaryStage() {
		return primaryStage;
	}

	/**
	 * Return root layout.
	 * 
	 * @return
	 */
	public BorderPane getRootLayout() {
		return rootLayout;
	}

	/**
	 * Return root layout controller.
	 * 
	 * @return
	 */
	public RootLayoutController getRootLayoutController() {
		return rootLayoutController;
	}

	/**
	 * Return currently active controller.
	 * 
	 * @return
	 */
	public AbstractController getController() {
		return currentController;
	}
}