package de.uni.hannover.studip.sync.models.jobs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;

import org.scribe.exceptions.OAuthConnectionException;

import de.elanev.studip.android.app.backend.datamodel.Document;
import de.elanev.studip.android.app.backend.datamodel.DocumentFolder;
import de.elanev.studip.android.app.backend.datamodel.DocumentFolders;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentFolderTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentTreeNode;
import de.uni.hannover.studip.sync.exceptions.ForbiddenException;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.models.TreeBuilder;
import de.uni.hannover.studip.sync.models.TreeConflict;
import javafx.application.Platform;

/**
 * Build files job.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class BuildDocumentsJob implements Runnable {
	
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
	 * Course node.
	 */
	private final CourseTreeNode courseNode;
	
	/**
	 * Parent folder node.
	 */
	private final DocumentFolderTreeNode parentNode;

	/**
	 * File index for duplicate check.
	 */
	private final Set<String> fileIndex;

	/**
	 * Constructor.
	 * 
	 * @param phaser
	 * @param courseNode Course tree-node
	 * @param parentNode Folder tree-node
	 */
	public BuildDocumentsJob(final TreeBuilder builder, final Phaser phaser, final CourseTreeNode courseNode, final DocumentFolderTreeNode parentNode, final Set<String> fileIndex) {
		this.builder = builder;
		this.phaser = phaser;
		this.courseNode = courseNode;
		this.parentNode = parentNode;
		this.fileIndex = fileIndex;
	}

	@Override
	public void run() {
		try {
			/* Folder merges must be mutually exclusive. */
			synchronized (fileIndex) {
				DocumentFolderTreeNode folderNode;
				DocumentTreeNode documentNode;

				final HashMap<String, Set<String>> fileIndexMap = new HashMap<>();

				/*
				 * Get course folder content.
				 * If parent node is the root course folder the folder id is null.
				 */
				final DocumentFolders folders = RestApi.getAllDocumentsByRangeAndFolderId(courseNode.courseId, parentNode.folderId);
				phaser.bulkRegister(folders.subfolders.size());

				/* Folders. */
				for (final DocumentFolder folder : folders.subfolders) {
					/* Get folder index (merged folders use same index) and rename the folder if it's name already exists. */
					final Set<String> folderFileIndex = TreeConflict.resolveFolderNameConflict(fileIndex, fileIndexMap, folder);
					parentNode.folders.add(folderNode = new DocumentFolderTreeNode(folder));

					builder.execute(new BuildDocumentsJob(builder, phaser, courseNode, folderNode, folderFileIndex));

					LOG.info(folderNode.name);
				}

				/* Documents. */
				for (final Document document : folders.file_refs) {
					/* Rename the document if it's filename already exists. */
					TreeConflict.resolveFileNameConflict(fileIndex, document);
					parentNode.documents.add(documentNode = new DocumentTreeNode(document));

					LOG.info(documentNode.name);
				}
			}

		} catch (OAuthConnectionException | IOException | RejectedExecutionException e) {
			/* Connection failed. */
			builder.stopPending = true;

		} catch (UnauthorizedException e) {
			/* Invalid oauth access token. */
			Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
			builder.stopPending = true;

		} catch (ForbiddenException | NotFoundException e) {
			/*
			 * User does not have the required permissions
			 * or folder does not exist.
			 */
			throw new IllegalStateException(e);

		} finally {
			/* Job done. */
			if (builder.stopPending || Main.exitPending) {
				phaser.forceTermination();
				builder.shutdownNow();
			} else {
				builder.updateProgressLabel(courseNode.title);
				phaser.arrive();
			}
		}
	}
}
