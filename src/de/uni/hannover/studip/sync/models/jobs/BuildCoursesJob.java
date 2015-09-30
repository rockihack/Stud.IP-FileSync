package de.uni.hannover.studip.sync.models.jobs;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import javafx.application.Platform;

import org.scribe.exceptions.OAuthConnectionException;

import de.elanev.studip.android.app.backend.datamodel.Course;
import de.elanev.studip.android.app.backend.datamodel.Courses;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.models.TreeBuilder;

/**
 * Build courses job.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class BuildCoursesJob implements Runnable {
	
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
	 * Semester node.
	 */
	private final SemesterTreeNode semesterNode;

	/**
	 * Constructor.
	 * 
	 * @param phaser
	 * @param semesterNode Semester tree-node
	 */
	public BuildCoursesJob(final TreeBuilder builder, final Phaser phaser, final SemesterTreeNode semesterNode) {
		this.builder = builder;
		this.phaser = phaser;
		this.semesterNode = semesterNode;
	}

	@Override
	public void run() {
		try {
			CourseTreeNode courseNode;

			/* Get subscribed courses. */
			final Courses courses = RestApi.getAllCoursesBySemesterId(semesterNode.semesterId);
			phaser.bulkRegister(courses.courses.size());
			
			for (final Course course : courses.courses) {
				semesterNode.courses.add(courseNode = new CourseTreeNode(course));
				
				/* Add build documents job. */
				builder.execute(new BuildDocumentsJob(builder, phaser, courseNode, courseNode.root, new HashSet<String>()));
				
				LOG.info(courseNode.title);
			}

		} catch (OAuthConnectionException e) {
			/* Connection failed. */
			builder.setStopPending();

		} catch (UnauthorizedException e) {
			/* Invalid oauth access token. */
			Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
			builder.setStopPending();

		} catch (NotFoundException e) {
			/* Course does not exist. */
			throw new IllegalStateException(e);

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
				builder.updateProgressLabel(semesterNode.title);
				phaser.arrive();
			}
		}
	}
}
