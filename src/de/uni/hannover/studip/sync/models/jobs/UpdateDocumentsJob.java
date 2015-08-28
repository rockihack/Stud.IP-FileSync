package de.uni.hannover.studip.sync.models.jobs;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;

import org.scribe.exceptions.OAuthConnectionException;

import de.elanev.studip.android.app.backend.datamodel.Document;
import de.elanev.studip.android.app.backend.datamodel.Documents;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentFolderTreeNode;
import de.uni.hannover.studip.sync.datamodel.DocumentTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.exceptions.ForbiddenException;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;
import de.uni.hannover.studip.sync.models.TreeBuilder;
import de.uni.hannover.studip.sync.models.TreeConflict;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileBrowser;

/**
 * Update files job.
 * 
 * @author Lennart Glauer
 */
public class UpdateDocumentsJob implements Runnable {

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
	 * Course node.
	 */
	private final CourseTreeNode courseNode;

	/**
	 * Current unix timestamp.
	 */
	private final long now;

	/**
	 * Constructor.
	 * 
	 * @param phaser
	 * @param semesterNode Semester tree-node
	 * @param courseNode Course tree-node
	 * @param now Current unix timestamp
	 */
	public UpdateDocumentsJob(final TreeBuilder builder, final Phaser phaser, final SemesterTreeNode semesterNode, final CourseTreeNode courseNode, final long now) {
		this.builder = builder;
		this.phaser = phaser;
		this.semesterNode = semesterNode;
		this.courseNode = courseNode;
		this.now = now;
	}

	/**
	 * Build folder index, so we can access the folder nodes in constant time.
	 * 
	 * @param folderIndex Folder index
	 * @param parentFolder Foler tree-node
	 */
	private static void buildFolderIndex(final Map<String, DocumentFolderTreeNode> folderIndex, final Map<String, DocumentFolderTreeNode> parentIndex, final DocumentFolderTreeNode parentFolder) {
		for (DocumentFolderTreeNode folder : parentFolder.folders) {
			parentIndex.put(folder.folderId, parentFolder);
			buildFolderIndex(folderIndex, parentIndex, folder);
		}

		folderIndex.put(parentFolder.folderId, parentFolder);
	}

	/**
	 * Remove document node if it exists.
	 * 
	 * @param folderNode Parent folder tree-node
	 * @param document Document to remove
	 * @return True if document was removed
	 */
	private static boolean removeDocument(final DocumentFolderTreeNode folderNode, final Document document) {
		final Iterator<DocumentTreeNode> iter = folderNode.documents.iterator();

		while (iter.hasNext()) {
			final DocumentTreeNode doc = iter.next();

			if (document.document_id.equals(doc.documentId)) {
				iter.remove();
				return true;
			}
		}

		return false;
	}

	/**
	 * Resolve file name conflicts.
	 * Ignore case because Windows and MacOS filesystems are case insensitive.
	 * 
	 * @param parentIndex
	 * @param folderNode Parent folder tree-node
	 * @param document Document to compare
	 * @return True if duplicate exists
	 */
	private static void resolveFileNameConflict(final Map<String, DocumentFolderTreeNode> parentIndex, final DocumentFolderTreeNode folderNode, final Document document) {
		final DocumentFolderTreeNode parentNode = parentIndex.get(folderNode.folderId);
		final Set<String> fileIndex = new HashSet<String>();

		/* Merge default folder with parent. */
		if (StudIPApiProvider.DEFAULT_FOLDER.equals(folderNode.name.trim())) {
			for (DocumentTreeNode doc : parentNode.documents) {
				fileIndex.add(FileBrowser.removeIllegalCharacters(doc.fileName).toLowerCase(Locale.GERMANY));
			}
			for (DocumentFolderTreeNode folder : parentNode.folders) {
				fileIndex.add(FileBrowser.removeIllegalCharacters(folder.name).toLowerCase(Locale.GERMANY));
			}
		}

		/* Build file index for merged folders. */
		final String folderName = FileBrowser.removeIllegalCharacters(folderNode.name);

		for (DocumentFolderTreeNode folder : parentNode.folders) {
			if (folderName.equalsIgnoreCase(FileBrowser.removeIllegalCharacters(folder.name))) {
				for (DocumentFolderTreeNode folder2 : folder.folders) {
					fileIndex.add(FileBrowser.removeIllegalCharacters(folder2.name).toLowerCase(Locale.GERMANY));
				}
				for (DocumentTreeNode doc : folder.documents) {
					fileIndex.add(FileBrowser.removeIllegalCharacters(doc.fileName).toLowerCase(Locale.GERMANY));
				}
			}
		}

		TreeConflict.resolveFileNameConflict(fileIndex, document);
	}

	@Override
	public void run() {
		try {
			DocumentFolderTreeNode folderNode;
			DocumentTreeNode documentNode;

			/* Get all course documents with newer change date than course update time. */
			final Documents newDocuments = RestApi.getNewDocumentsByCourseId(courseNode.courseId, courseNode.updateTime);
			if (!newDocuments.documents.isEmpty()) {
				/* Build a folder index for this course, so we can easily access the folders. */
				final Map<String, DocumentFolderTreeNode> folderIndex = new HashMap<String, DocumentFolderTreeNode>();
				final Map<String, DocumentFolderTreeNode> parentIndex = new HashMap<String, DocumentFolderTreeNode>();
				buildFolderIndex(folderIndex, parentIndex, courseNode.root);

				for (Document document : newDocuments.documents) {
					folderNode = folderIndex.get(document.folder_id);
					if (folderNode == null) {
						/* Folder does not exist locally, we need to re-sync all course folders. */
						phaser.register();
						builder.execute(new BuildDocumentsJob(builder, phaser, courseNode, courseNode.root = new DocumentFolderTreeNode(), new HashSet<String>()));
						break;
					}

					/*
					 * Maybe the document was updated and the node already exists,
					 * we need to replace the document node (remove + add).
					 */
					removeDocument(folderNode, document);

					resolveFileNameConflict(parentIndex, folderNode, document);

					/* Add document to existing folder. */
					folderNode.documents.add(documentNode = new DocumentTreeNode(document));

					LOG.info(documentNode.name);
				}
			}

			/* Update unix timestamp. */
			courseNode.updateTime = now;
			builder.setDirty();

		} catch (OAuthConnectionException e) {
			/* Connection failed. */
			builder.setStopPending();

		} catch (UnauthorizedException e) {
			/* Invalid oauth access token. */
			Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
			builder.setStopPending();

		} catch (ForbiddenException | NotFoundException e) {
			/*
			 * User does not have the required permissions
			 * or course does not exist.
			 */
			semesterNode.courses.remove(courseNode);
			builder.setDirty();

			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Removed course: " + courseNode.title);
			}

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
				builder.updateProgressLabel(courseNode.title);
				phaser.arrive();
			}
		}
	}
}
