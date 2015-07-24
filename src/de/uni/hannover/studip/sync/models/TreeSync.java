package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import javafx.application.Platform;

import org.scribe.exceptions.OAuthConnectionException;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.utils.FileBrowser;

/**
 * Semester/Course/Folder/Document tree sync.
 * 
 * @author Lennart Glauer
 */
public class TreeSync extends TreeBuilder {

	private static final Config CONFIG = Config.getInstance();

	/**
	 * The sync root directory.
	 */
	private final Path rootDirectory;

	/**
	 * Constructor.
	 * 
	 * @param rootDirectory Path to sync root directory
	 */
	public TreeSync(final Path rootDirectory) {
		// Start threadpool in super class.
		super();

		if (!Files.isDirectory(rootDirectory)) {
			throw new IllegalStateException("Root directory does not exist!");
		}

		this.rootDirectory = rootDirectory;
	}

	/**
	 * Synchronize all documents.
	 * 
	 * @param tree Path to tree file
	 * @param doAllSemesters If true documents from all semesters will be downloaded, otherwise only from current semester
	 * @throws IOException
	 */
	public synchronized int sync(final Path tree, final boolean doAllSemesters) throws IOException {
		if (stopPending || Main.exitPending) {
			return 0;
		}

		/* Read existing tree. */
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(Files.newBufferedReader(tree), SemestersTreeNode.class);

		final Phaser phaser = new Phaser(1); /* = self. */
		final long now = System.currentTimeMillis() / 1000L;
		final String folderStructure = CONFIG.getFolderStructure();

		isDirty = false;

		/* Sync tree with multiple threads. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			/* If doAllSemesters is false we will only update the current semester. */
			if (doAllSemesters || (now > semester.begin && now < semester.end + SEMESTER_THRESHOLD)) {
				for (CourseTreeNode course : semester.courses) {
					final Path courseDirectory = new PathBuilder(folderStructure, rootDirectory, semester, course).toPath();

					if (!Files.isDirectory(courseDirectory)) {
						Files.createDirectories(courseDirectory);
					}

					doFolder(phaser, course.root, courseDirectory);
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

			LOG.info("Sync done!");
		}

		return phaser.getRegisteredParties() - 1;
	}

	/**
	 * Folder node handler.
	 * 
	 * @param phaser
	 * @param folderNode Folder tree-node
	 * @param parentDirectory Path to parent directory
	 * @throws IOException 
	 */
	private void doFolder(final Phaser phaser, final DocumentFolderTreeNode folderNode, final Path parentDirectory) throws IOException {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			final Path folderDirectory = parentDirectory.resolve(FileBrowser.removeIllegalCharacters(folder.name));

			if (!Files.isDirectory(folderDirectory)) {
				Files.createDirectory(folderDirectory);
			}

			doFolder(phaser, folder, folderDirectory);
		}

		for (DocumentTreeNode document : folderNode.documents) {
			doDocument(phaser, folderNode, document, parentDirectory);
		}
	}

	/**
	 * Document node handler.
	 * 
	 * @param phaser
	 * @param folderNode Parent folder tree-node
	 * @param documentNode Document tree-node
	 * @param parentDirectory Path to parent directory
	 * @throws IOException 
	 */
	private void doDocument(final Phaser phaser, final DocumentFolderTreeNode folderNode, final DocumentTreeNode documentNode, final Path parentDirectory) throws IOException {
		final String originalFileName = FileBrowser.removeIllegalCharacters(documentNode.fileName);
		final Path documentFile = parentDirectory.resolve(originalFileName);

		if (!Files.exists(documentFile)) {
			phaser.register();

			/* Download new file. */
			threadPool.execute(new DownloadDocumentJob(phaser, folderNode, documentNode, documentFile));

			if (LOG.isLoggable(Level.INFO)) {
				LOG.info("New: " + originalFileName);
			}

		} else if (Files.size(documentFile) != documentNode.fileSize || Files.getLastModifiedTime(documentFile).toMillis() != documentNode.chDate * 1000L) {
			/* Document has changed, we will download it again. */

			if (!CONFIG.isOverwriteFiles()) {
				/* Overwrite files is disabled, we append a version number to the old document filename. */
				Path renameFile;
				int i = 0;

				do {
					i++;
					renameFile = parentDirectory.resolve(FileBrowser.appendFilename(originalFileName, "_v" + i));
				} while(Files.exists(renameFile));

				Files.move(documentFile, renameFile);

				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("Renamed: " + originalFileName + " to " + renameFile.getFileName());
				}
			}

			phaser.register();

			/* Download modified file. */
			threadPool.execute(new DownloadDocumentJob(phaser, folderNode, documentNode, documentFile));

			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Modified: " + originalFileName);
			}
		}
	}
	
	/**
	 * Download document job.
	 * 
	 * @author Lennart Glauer
	 */
	private class DownloadDocumentJob implements Runnable {

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
		public DownloadDocumentJob(final Phaser phaser, final DocumentFolderTreeNode folderNode, final DocumentTreeNode documentNode, final Path documentFile) {
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

			} catch (OAuthConnectionException e) {
				/* Connection failed. */
				stopPending = true;

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
				stopPending = true;

			} catch (ForbiddenException | NotFoundException e) {
				/*
				 * User does not have the required permissions
				 * or document does not exist.
				 */
				folderNode.documents.remove(documentNode);
				isDirty = true;

				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("Removed document: " + documentNode.fileName);
				}

			} catch (IOException e) {
				throw new IllegalStateException(e);

			} catch (RejectedExecutionException e) {
				if (!stopPending && !Main.exitPending) {
					throw new IllegalStateException(e);
				}

			} finally {
				/* Job done. */
				// TODO: Add course name in new line.
				updateProgressLabel(documentNode.name);
				phaser.arrive();

				if (stopPending || Main.exitPending) {
					phaser.forceTermination();
				}
			}
		}
	}
}