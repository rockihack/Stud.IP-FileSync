package de.uni.hannover.studip.sync.models.jobs;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import javafx.application.Platform;

import org.scribe.exceptions.OAuthConnectionException;

import de.elanev.studip.android.app.backend.datamodel.Semester;
import de.elanev.studip.android.app.backend.datamodel.Semesters;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.models.TreeBuilder;

/**
 * Build semesters job.
 * 
 * @author Lennart Glauer
 */
public class BuildSemestersJob implements Runnable {
	
	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	/**
	 * TreeBuilder.
	 */
	private final TreeBuilder builder;
	
	/**
	 * Phaser.
	 */
	private final Phaser phaser;
	
	/**
	 * Tree root node.
	 */
	private final SemestersTreeNode rootNode;

	/**
	 * Constructor.
	 * 
	 * @param phaser
	 * @param rootNode Root tree-node
	 */
	public BuildSemestersJob(final TreeBuilder builder, final Phaser phaser, final SemestersTreeNode rootNode) {
		this.builder = builder;
		this.phaser = phaser;
		this.rootNode = rootNode;
	}

	@Override
	public void run() {
		try {
			SemesterTreeNode semesterNode;

			/* Get all visible semesters. */
			final Semesters semesters = RestApi.getAllSemesters();
			phaser.bulkRegister(semesters.semesters.size());

			for (Semester semester : semesters.semesters) {
				rootNode.semesters.add(semesterNode = new SemesterTreeNode(semester));

				/* Add build courses job. */
				builder.execute(new BuildCoursesJob(builder, phaser, semesterNode));

				LOG.info(semesterNode.title);
			}

		} catch (OAuthConnectionException e) {
			/* Connection failed. */
			builder.setStopPending();

		} catch (UnauthorizedException e) {
			/* Invalid oauth access token. */
			Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
			builder.setStopPending();

		} catch (IOException e) {
			throw new IllegalStateException(e);

		} catch (RejectedExecutionException e) {
			if (!builder.isStopPending() && !Main.exitPending) {
				throw new IllegalStateException(e);
			}

		} finally {
			/* Job done. */
			if (builder.isStopPending() || Main.exitPending) {
				phaser.forceTermination();
				builder.shutdownNow();
			} else {
				phaser.arrive();
			}
		}
	}
}