package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Phaser;
import java.util.logging.Level;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.models.jobs.DownloadDocumentJob;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileBrowser;

/**
 * Semester/Course/Folder/Document tree sync.
 * 
 * @author Lennart Glauer
 */
public class TreeSync extends TreeBuilder {

	private static final Config CONFIG = Config.getInstance();
	private static final RenameMap RENAMEMAP = RenameMap.getInstance();

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
		/* Start threadpool in super class. */
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
		for (final SemesterTreeNode semester : rootNode.semesters) {
			/* If doAllSemesters is false we will only sync the current semester. */
			if (doAllSemesters || (now > semester.begin && now < semester.end + SEMESTER_THRESHOLD)) {
				for (final CourseTreeNode course : semester.courses) {
					
					final Path courseDirectory = PathBuilder.toPath(folderStructure, rootDirectory, semester, course);
					final Path renamedCourseDirectory = rootDirectory.resolve(RENAMEMAP.checkPath(rootDirectory.relativize(courseDirectory).toString()));
					
					if (!Files.isDirectory(renamedCourseDirectory)) {
						Files.createDirectories(renamedCourseDirectory);
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
		for (final DocumentFolderTreeNode folder : folderNode.folders) {
			if (StudIPApiProvider.DEFAULT_FOLDER.equals(folder.name.trim())) {
				/* Merge default folder with parent. */
				doFolder(phaser, folder, parentDirectory);
				continue;
			}

			final Path folderDirectory = parentDirectory.resolve(FileBrowser.removeIllegalCharacters(folder.name));
			final Path renamedFolderDirectory = rootDirectory.resolve(RENAMEMAP.checkPath(rootDirectory.relativize(folderDirectory).toString()));

			if (!Files.isDirectory(renamedFolderDirectory)) {
				Files.createDirectory(renamedFolderDirectory);
			}

			doFolder(phaser, folder, folderDirectory);
		}

		for (final DocumentTreeNode document : folderNode.documents) {
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
		//final String originalFileName = FileBrowser.removeIllegalCharacters(documentNode.fileName);
		//Path documentFile = parentDirectory.resolve(originalFileName);
		//documentFile = rootDirectory.resolve(RENAMEMAP.checkPath(rootDirectory.relativize(documentFile).toString()));
		String originalFileName = FileBrowser.removeIllegalCharacters(documentNode.fileName);
		Path documentFile = parentDirectory.resolve(originalFileName);
		final String renamedPath = RENAMEMAP.checkPath(rootDirectory.relativize(documentFile).toString());
		documentFile = rootDirectory.resolve(renamedPath);
		String documentFileParts[] = renamedPath.split(File.separator);
		originalFileName = documentFileParts[documentFileParts.length-1];

		if (!Files.exists(documentFile)) {
			phaser.register();

			/* Download new file. */
			threadPool.execute(new DownloadDocumentJob(this, phaser, folderNode, documentNode, documentFile));

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
			threadPool.execute(new DownloadDocumentJob(this, phaser, folderNode, documentNode, documentFile));

			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Modified: " + originalFileName);
			}
		}
	}
}