package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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

	/**
	 * Config instance.
	 */
	private static final Config CONFIG = Config.getInstance();

	/**
	 * The sync root directory.
	 */
	private final File rootDirectory;

	/**
	 * Constructor.
	 * 
	 * @param rootDirectory
	 */
	public TreeSync(final File rootDirectory) {
		// Start threadpool in super class.
		super();

		if (!rootDirectory.isDirectory()) {
			throw new IllegalStateException("Root directory does not exist!");
		}
		
		this.rootDirectory = rootDirectory;
	}
	
	/**
	 * Synchronize all documents.
	 * 
	 * @param tree
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized int sync(final File tree, final boolean doAllSemesters) throws IOException {
		if (Main.stopPending) {
			return 0;
		}

		/* Read existing tree. */
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(tree, SemestersTreeNode.class);

		/* A phaser is actually a up and down latch, it's used to wait until all jobs are done. */
		final Phaser phaser = new Phaser(1); /* = self. */

		/* Current unix timestamp. */
		final long now = System.currentTimeMillis() / 1000L;

		isDirty = false;

		/* Sync tree with multiple threads. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			/* If doAllSemesters is false we will only update the current semester. */
			if (doAllSemesters || (now > semester.begin && now < semester.end + SEMESTER_THRESHOLD)) {
				final File semesterDirectory = new File(rootDirectory, FileBrowser.removeIllegalCharacters(semester.title));

				if (!semesterDirectory.exists() && !semesterDirectory.mkdir()) {
					throw new IllegalStateException("Could not create semester directory!");
				}

				for (CourseTreeNode course : semester.courses) {
					final File courseDirectory = new File(semesterDirectory, FileBrowser.removeIllegalCharacters(course.title));

					if (!courseDirectory.exists() && !courseDirectory.mkdir()) {
						throw new IllegalStateException("Could not create course directory!");
					}

					doFolder(phaser, course.root, courseDirectory);
				}
			}
		}

		startProgressAnimation(phaser);

		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();

		if (!Main.stopPending) {
			if (isDirty) {
				/* Serialize the tree to json and store it in the tree file. */
				mapper.writeValue(tree, rootNode);
			}

			LOG.info("Sync done!");
		}

		return phaser.getRegisteredParties() - 1;
	}

	/**
	 * Folder node handler.
	 * 
	 * @param phaser
	 * @param folderNode The folder node
	 * @param parentDirectory The parent directory
	 */
	private void doFolder(final Phaser phaser, final DocumentFolderTreeNode folderNode, final File parentDirectory) {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : folderNode.folders) {
			final File folderDirectory = new File(parentDirectory, FileBrowser.removeIllegalCharacters(folder.name));

			if (!folderDirectory.exists() && !folderDirectory.mkdir()) {
				throw new IllegalStateException("Could not create course directory!");
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
	 * @param folderNode
	 * @param documentNode
	 * @param parentDirectory
	 */
	private void doDocument(final Phaser phaser, final DocumentFolderTreeNode folderNode, final DocumentTreeNode documentNode, final File parentDirectory) {
		final String originalFileName = FileBrowser.removeIllegalCharacters(documentNode.fileName);
		final File documentFile = new File(parentDirectory, originalFileName);

		if (!documentFile.exists()) {
			phaser.register();

			/* Download new file. */
			threadPool.execute(new DownloadDocumentJob(phaser, folderNode, documentNode, documentFile));

			if (LOG.isLoggable(Level.INFO)) {
				LOG.info("New: " + documentNode.name);
			}

		} else if (documentFile.length() != documentNode.fileSize || documentFile.lastModified() != documentNode.chDate * 1000L) {
			/* Document has changed, we will download it again. */

			if (!CONFIG.isOverwriteFiles()) {
				/* Overwrite files is disabled, we append a version number to the old document filename. */
				File renameFile;
				int i = 0;

				do {
					i++;
					renameFile = new File(parentDirectory, FileBrowser.appendFilename(originalFileName, "_v" + i));
				} while(renameFile.exists());

				if (!documentFile.renameTo(renameFile)) {
					throw new IllegalStateException("Datei konnte nicht umbenannt werden.\n" + documentFile.getAbsolutePath());
				}

				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("Renamed: " + documentNode.name + " to " + renameFile.getName());
				}
			}

			phaser.register();

			/* Download modified file. */
			threadPool.execute(new DownloadDocumentJob(phaser, folderNode, documentNode, documentFile));

			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Modified: " + documentNode.name);
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
		private final File documentFile;

		/**
		 * Download document job.
		 * 
		 * @param phaser
		 * @param documentNode The document node to download
		 * @param documentFile The file location to store the document
		 */
		public DownloadDocumentJob(final Phaser phaser, final DocumentFolderTreeNode folderNode, final DocumentTreeNode documentNode, final File documentFile) {
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
				if (!documentFile.setLastModified(documentNode.chDate * 1000L)) {
					throw new IllegalStateException("Änderungsdatum konnte nicht gesetzt werden!");
				}

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				Main.stopPending = true;

				OAuth.getInstance().removeAccessToken();

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
				if (!Main.stopPending) {
					throw new IllegalStateException(e);
				}

			} finally {
				/* Job done. */
				// TODO: Add course name in new line.
				updateProgressLabel(documentNode.name);
				phaser.arrive();

				if (Main.stopPending) {
					phaser.forceTermination();
				}
			}
		}
	}
}