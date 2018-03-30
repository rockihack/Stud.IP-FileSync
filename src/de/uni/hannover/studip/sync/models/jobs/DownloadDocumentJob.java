package de.uni.hannover.studip.sync.models.jobs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.scribe.exceptions.OAuthConnectionException;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.DocumentFolderTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentTreeNode;
import de.uni.hannover.studip.sync.exceptions.ForbiddenException;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.models.TreeSync;
import javafx.application.Platform;

/**
 * Download document job.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class DownloadDocumentJob implements Runnable {

	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	/**
	 * TreeBuilder.
	 */
	private final TreeSync sync;

	/**
	 * Phaser.
	 */
	private final Phaser phaser;

	/**
	 * Folder node.
	 */
	private final DocumentFolderTreeNode folderNode;

	/**
	 * Document node.
	 * The document node to download.
	 */
	private final DocumentTreeNode documentNode;

	/**
	 * Document file.
	 * The file location to store the document.
	 */
	private final Path documentFile;

	/**
	 * Download document job.
	 * 
	 * @param phaser
	 * @param folderNode Parent folder tree-node
	 * @param documentNode Document tree-node to download
	 * @param documentFile Path to document file destination
	 */
	public DownloadDocumentJob(final TreeSync sync, final Phaser phaser, final DocumentFolderTreeNode folderNode, final DocumentTreeNode documentNode, final Path documentFile) {
		this.sync = sync;
		this.phaser = phaser;
		this.folderNode = folderNode;
		this.documentNode = documentNode;
		this.documentFile = documentFile;
	}

	@Override
	public void run() {
		try {
			final long startTime = System.currentTimeMillis();
			RestApi.downloadDocumentById(documentNode.documentId, documentFile);
			final long endTime = System.currentTimeMillis();

			if (LOG.isLoggable(Level.INFO)) {
				LOG.info("Downloaded " + documentFile + " in " + (endTime - startTime) + "ms");
			}

			/*
			 * We use the last modified timestamp to detect file changes.
			 * The timestamp must be the same as in the document node,
			 * otherwise the file will be downloaded again.
			 */
			Files.setLastModifiedTime(documentFile, FileTime.fromMillis(documentNode.chDate * 1000L));

		} catch (OAuthConnectionException | IOException | RejectedExecutionException e) {
			/* Connection failed. */
			sync.stopPending = true;

		} catch (UnauthorizedException e) {
			/* Invalid oauth access token. */
			Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
			sync.stopPending = true;

		} catch (ForbiddenException | NotFoundException e) {
			/*
			 * User does not have the required permissions
			 * or document does not exist.
			 */
			folderNode.documents.remove(documentNode);
			sync.isDirty = true;

			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Removed document: " + documentNode.name);
			}

		} finally {
			/* Job done. */
			if (sync.stopPending || Main.exitPending) {
				phaser.forceTermination();
				sync.shutdownNow();
			} else {
				// TODO: Add course name in new line.
				sync.updateProgressLabel(documentNode.name);
				phaser.arrive();
			}
		}
	}
}
