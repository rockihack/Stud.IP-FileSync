package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.logging.Logger;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.models.jobs.BuildSemestersJob;

/**
 * Semester/Course/Folder/Document tree builder.
 * 
 * @author Lennart Glauer
 */
public class TreeBuilder implements AutoCloseable {

	protected static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	protected static final ObjectMapper MAPPER = Config.getMapper();

	/**
	 * Thread pool.
	 */
	protected final ExecutorService threadPool;

	/**
	 * Flag to signal graceful shutdown of worker threads.
	 */
	public volatile boolean stopPending;

	/**
	 * Flag to signal if the tree is dirty and needs to be written to disk.
	 */
	public volatile boolean isDirty;

	/**
	 * Gui progress indicator.
	 */
	protected ProgressIndicator progressIndicator;

	/**
	 * Gui progress label.
	 */
	private Label progressLabel;

	/**
	 * Start threadpool.
	 */
	protected TreeBuilder() {
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Stop threadpool.
	 */
	@Override
	public void close() {
		threadPool.shutdownNow();
	}

	/**
	 * Build the semester/course/folder/document tree and store it in json format.
	 * 
	 * This method always creates a new tree!
	 * 
	 * @param tree Path to tree file
	 * @throws IOException
	 */
	public synchronized int build(final Path tree) throws IOException {
		if (stopPending || Main.exitPending) {
			return 0;
		}

		/* Create empty root node. */
		final SemestersTreeNode rootNode = new SemestersTreeNode();
		final Phaser phaser = new Phaser(2); /* = self + first job. */

		/* Build tree with multiple threads. */
		threadPool.execute(new BuildSemestersJob(this, phaser, rootNode));

		startProgressAnimation(phaser);

		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();

		if (!stopPending && !Main.exitPending) {
			/* Serialize the tree to json and store it in the tree file. */
			MAPPER.writerFor(SemestersTreeNode.class)
					.writeValue(Files.newOutputStream(tree), rootNode);

			LOG.info("Build done!");
		}

		return phaser.getRegisteredParties() - 1;
	}
	
	/**
	 * Update existing semester/course/folder/document tree.
	 * 
	 * @param tree Path to tree file
	 * @param doAllSemesters If true all semesters will be updated, otherwise only the current semester
	 * @throws IOException
	 */
	public synchronized int update(final Path tree) throws IOException {
		// TODO
		return build(tree);
	}

	public void execute(final Runnable job) {
		threadPool.execute(job);
	}

	public void shutdownNow() {
		threadPool.shutdownNow();
	}

	/**
	 * Set gui progress indicator and label.
	 * 
	 * @param progress Progress indicator
	 * @param label Progress label
	 */
	public void setProgress(final ProgressIndicator progress, final Label label) {
		progressIndicator = progress;
		progressLabel = label;
	}

	/**
	 * Update gui progress label.
	 * 
	 * @param text Progress status
	 */
	public void updateProgressLabel(final String text) {
		if (progressLabel != null) {
			Platform.runLater(() -> progressLabel.setText(text));
		}
	}

	/**
	 * Start gui progress animation.
	 * 
	 * @param phaser
	 */
	protected void startProgressAnimation(final Phaser phaser) {
		if (progressIndicator != null) {
			if (phaser.getRegisteredParties() < 2) {
				progressIndicator.setProgress(1);

			} else {
				(new AnimationTimer() {
					private double y;

					@Override
					public void handle(final long now) {
						final int a = phaser.getArrivedParties() - 1;
						final int r = phaser.getRegisteredParties() - 1;
						final double x = phaser.getPhase() == 0 ? Math.min(0.02 * a * a, (double) a / r) : 1.2;

						if (y <= x) {
							progressIndicator.setProgress(y += 0.1 * (x - y));
						}

						if (y >= 1.0) {
							stop();
						}
					}
				}).start();
			}
		}
	}
}