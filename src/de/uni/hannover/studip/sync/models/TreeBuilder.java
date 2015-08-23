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

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.models.jobs.BuildSemestersJob;
import de.uni.hannover.studip.sync.models.jobs.UpdateDocumentsJob;

/**
 * Semester/Course/Folder/Document tree builder.
 * 
 * @author Lennart Glauer
 */
public class TreeBuilder implements AutoCloseable {

	protected static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * Time in seconds a semester will be updated after it's end date.
	 */
	protected static final long SEMESTER_THRESHOLD = 30 * 24 * 60 * 60;

	/**
	 * Thread pool.
	 */
	protected final ExecutorService threadPool;

	/**
	 * Flag to signal graceful shutdown of worker threads.
	 */
	protected volatile boolean stopPending;

	/**
	 * Signals if the tree is dirty and needs to be written to disk.
	 */
	protected volatile boolean isDirty;

	/**
	 * Gui progress indicator.
	 */
	private ProgressIndicator progressIndicator;

	/**
	 * Gui progress label.
	 */
	private Label progressLabel;

	/**
	 * Start the threadpool.
	 */
	protected TreeBuilder() {
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Stops the threadpool.
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
			final ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(Files.newBufferedWriter(tree), rootNode);

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
	public synchronized int update(final Path tree, final boolean doAllSemesters) throws IOException {
		if (stopPending || Main.exitPending) {
			return 0;
		}

		/* Read existing tree. */
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(Files.newBufferedReader(tree), SemestersTreeNode.class);

		if (rootNode.semesters.isEmpty()) {
			throw new JsonMappingException("No semesters found!");
		}

		final Phaser phaser = new Phaser(1); /* = self. */
		final long now = System.currentTimeMillis() / 1000L;

		isDirty = false;

		/* Update tree with multiple threads. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			/* If doAllSemesters is false we will only update the current semester. */
			if (doAllSemesters || (now > semester.begin && now < semester.end + SEMESTER_THRESHOLD)) {
				for (CourseTreeNode course : semester.courses) {
					/* Request caching. */
					if (now - course.updateTime > StudIPApiProvider.CACHE_TIME) {
						phaser.register();

						/*
						 * If Rest.IP plugin 0.9.9.6 or later is installed we can use UpdateDocumentsJob.
						 * Since this version the api offers a more efficient route for updating documents,
						 * otherwise we need to rebuild the folder tree every time.
						 */
						threadPool.execute(new UpdateDocumentsJob(this, phaser, semester, course, now));
					}
				}
			}
		}

		startProgressAnimation(phaser);

		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();

		if (!stopPending && !Main.exitPending) {
			if (isDirty) {
				/* Serialize the tree to json and store it in the tree file. */
				mapper.writeValue(Files.newBufferedWriter(tree), rootNode);
			}

			LOG.info("Update done!");
		}

		return phaser.getRegisteredParties() - 1;
	}

	public void execute(final Runnable job) {
		threadPool.execute(job);
	}
	
	public void shutdownNow() {
		threadPool.shutdownNow();
	}
	
	public void setStopPending() {
		stopPending = true;
	}
	
	public boolean isStopPending() {
		return stopPending;
	}

	public void setDirty() {
		isDirty = true;
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