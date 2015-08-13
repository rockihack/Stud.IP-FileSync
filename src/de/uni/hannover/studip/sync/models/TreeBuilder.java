package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.scribe.exceptions.OAuthConnectionException;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.elanev.studip.android.app.backend.datamodel.*;
import de.uni.hannover.studip.sync.Main;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileBrowser;

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
	protected ProgressIndicator progressIndicator;

	/**
	 * Gui progress label.
	 */
	protected Label progressLabel;

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
		threadPool.execute(new BuildSemestersJob(phaser, rootNode));

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
						threadPool.execute(new UpdateDocumentsJob(phaser, semester, course, now));
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
	
	/**
	 * Build semesters job.
	 * 
	 * @author Lennart Glauer
	 */
	private class BuildSemestersJob implements Runnable {
		
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
		public BuildSemestersJob(final Phaser phaser, final SemestersTreeNode rootNode) {
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
					threadPool.execute(new BuildCoursesJob(phaser, semesterNode));

					LOG.info(semesterNode.title);
				}

			} catch (OAuthConnectionException e) {
				/* Connection failed. */
				stopPending = true;

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
				stopPending = true;

			} catch (IOException e) {
				throw new IllegalStateException(e);

			} catch (RejectedExecutionException e) {
				if (!stopPending && !Main.exitPending) {
					throw new IllegalStateException(e);
				}

			} finally {
				/* Job done. */
				if (stopPending || Main.exitPending) {
					phaser.forceTermination();
					threadPool.shutdownNow();
				} else {
					phaser.arrive();
				}
			}
		}
	}
	
	/**
	 * Build courses job.
	 * 
	 * @author Lennart Glauer
	 */
	private class BuildCoursesJob implements Runnable {
		
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
		public BuildCoursesJob(final Phaser phaser, final SemesterTreeNode semesterNode) {
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
				
				for (Course course : courses.courses) {
					semesterNode.courses.add(courseNode = new CourseTreeNode(course));
					
					/* Add build documents job. */
					threadPool.execute(new BuildDocumentsJob(phaser, courseNode, courseNode.root, new HashSet<String>()));
					
					LOG.info(courseNode.title);
				}

			} catch (OAuthConnectionException e) {
				/* Connection failed. */
				stopPending = true;

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				Platform.runLater(() -> OAuth.getInstance().removeAccessToken());
				stopPending = true;

			} catch (NotFoundException e) {
				/* Course does not exist. */
				throw new IllegalStateException(e);

			} catch (IOException e) {
				throw new IllegalStateException(e);

			} catch (RejectedExecutionException e) {
				if (!stopPending && !Main.exitPending) {
					throw new IllegalStateException(e);
				}

			} finally {
				/* Job done. */
				if (stopPending || Main.exitPending) {
					phaser.forceTermination();
					threadPool.shutdownNow();
				} else {
					updateProgressLabel(semesterNode.title);
					phaser.arrive();
				}
			}
		}
	}
	
	/**
	 * Build files job.
	 * 
	 * @author Lennart Glauer
	 */
	private class BuildDocumentsJob implements Runnable {

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
		public BuildDocumentsJob(final Phaser phaser, final CourseTreeNode courseNode, final DocumentFolderTreeNode parentNode, final Set<String> fileIndex) {
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

					final Map<String, Set<String>> fileIndexMap = new HashMap<String, Set<String>>();

					/*
					 * Get course folder content.
					 * If parent node is the root course folder the folder id is null.
					 */
					final DocumentFolders folders = RestApi.getAllDocumentsByRangeAndFolderId(courseNode.courseId, parentNode.folderId);
					phaser.bulkRegister(folders.folders.size());

					/* Folders. */
					for (DocumentFolder folder : folders.folders) {
						/* Get folder index (merged folders use the same index). */
						final Set<String> folderFileIndex = resolveFolderNameConflict(fileIndex, fileIndexMap, folder);

						parentNode.folders.add(folderNode = new DocumentFolderTreeNode(folder));

						/* Add update files job (recursive). */
						threadPool.execute(new BuildDocumentsJob(phaser, courseNode, folderNode, folderFileIndex));

						LOG.info(folderNode.name);
					}

					/* Documents. */
					for (Document document : folders.documents) {
						resolveFileNameConflict(fileIndex, document);

						parentNode.documents.add(documentNode = new DocumentTreeNode(document));

						LOG.info(documentNode.name);
					}
				}

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
				 * or folder does not exist.
				 */
				throw new IllegalStateException(e);

			} catch (IOException e) {
				throw new IllegalStateException(e);

			} catch (RejectedExecutionException e) {
				if (!stopPending && !Main.exitPending) {
					throw new IllegalStateException(e);
				}

			} finally {
				/* Job done. */
				if (stopPending || Main.exitPending) {
					phaser.forceTermination();
					threadPool.shutdownNow();
				} else {
					updateProgressLabel(courseNode.title);
					phaser.arrive();
				}
			}
		}
	}
	
	/**
	 * Update files job.
	 * 
	 * @author Lennart Glauer
	 */
	private class UpdateDocumentsJob implements Runnable {

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
		public UpdateDocumentsJob(final Phaser phaser, final SemesterTreeNode semesterNode, final CourseTreeNode courseNode, final long now) {
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
		private void buildFolderIndex(final Map<String, DocumentFolderTreeNode> folderIndex, final Map<String, DocumentFolderTreeNode> parentIndex, final DocumentFolderTreeNode parentFolder) {
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
		private boolean removeDocument(final DocumentFolderTreeNode folderNode, final Document document) {
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
		private void resolveFileNameConflictUpdate(final Map<String, DocumentFolderTreeNode> parentIndex, final DocumentFolderTreeNode folderNode, final Document document) {
			final DocumentFolderTreeNode parentNode = parentIndex.get(folderNode.folderId);
			final Set<String> fileIndex = new HashSet<String>();

			/* Redirect default folder content to course directory. */
			if (folderNode.name.trim().equals("Allgemeiner Dateiordner")) {
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

			resolveFileNameConflict(fileIndex, document);
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
							threadPool.execute(new BuildDocumentsJob(phaser, courseNode, courseNode.root = new DocumentFolderTreeNode(), new HashSet<String>()));
							break;
						}

						/*
						 * Maybe the document was updated and the node already exists,
						 * we need to replace the document node (remove + add).
						 */
						removeDocument(folderNode, document);

						resolveFileNameConflictUpdate(parentIndex, folderNode, document);

						/* Add document to existing folder. */
						folderNode.documents.add(documentNode = new DocumentTreeNode(document));

						LOG.info(documentNode.name);
					}
				}

				/* Update unix timestamp. */
				courseNode.updateTime = now;
				isDirty = true;

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
				 * or course does not exist.
				 */
				semesterNode.courses.remove(courseNode);
				isDirty = true;

				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("Removed course: " + courseNode.title);
				}

			} catch (IOException e) {
				throw new IllegalStateException(e);

			} catch (RejectedExecutionException e) {
				if (!stopPending && !Main.exitPending) {
					throw new IllegalStateException(e);
				}

			} finally {
				/* Job done. */
				if (stopPending || Main.exitPending) {
					phaser.forceTermination();
					threadPool.shutdownNow();
				} else {
					updateProgressLabel(courseNode.title);
					phaser.arrive();
				}
			}
		}
	}

	/**
	 * Resolve folder name conflicts.
	 * 
	 * @param folder
	 * @param fileIndexMap
	 */
	private static Set<String> resolveFolderNameConflict(final Set<String> fileIndex, final Map<String, Set<String>> fileIndexMap, final DocumentFolder folder) {
		/* Merge default folder with parent. */
		if (folder.name.trim().equals("Allgemeiner Dateiordner")) {
			return fileIndex;
		}

		/* Use lowercase name because Windows and MacOS filesystems are case insensitive. */
		String folderName = FileBrowser.removeIllegalCharacters(folder.name).toLowerCase(Locale.GERMANY);

		if (fileIndexMap.get(folderName) == null) {
			/* Folder does not exist yet. */
			synchronized (fileIndex) {
				if (fileIndex.contains(folderName)) {
					/* Resolve file name conflict. */
					if (LOG.isLoggable(Level.WARNING)) {
						LOG.warning("Folder/file name conflict: " + folderName);
					}

					folder.name = FileBrowser.appendFilename(folder.name, "_" + folder.folder_id);
					folderName = FileBrowser.removeIllegalCharacters(folder.name).toLowerCase(Locale.GERMANY);
				}

				fileIndex.add(folderName);
			}

			fileIndexMap.put(folderName, new HashSet<String>());

		} else {
			/* Merge folders. */
			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("Merge folders: " + folderName);
			}
		}

		return fileIndexMap.get(folderName);
	}

	/**
	 * Resolve file name conflicts.
	 * 
	 * @param fileIndex
	 * @param document
	 */
	private static void resolveFileNameConflict(final Set<String> fileIndex, final Document document) {
		/* Use lowercase name because Windows and MacOS filesystems are case insensitive. */
		String fileName = FileBrowser.removeIllegalCharacters(document.filename).toLowerCase(Locale.GERMANY);

		synchronized (fileIndex) {
			if (fileIndex.contains(fileName)) {
				/* File name already exists. */
				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("File name conflict: " + fileName);
				}

				/* 1. Append Stud.IP name. */
				if (!document.name.isEmpty() && !document.name.equals(document.filename)) {
					document.filename = FileBrowser.appendFilename(document.filename, "_(" + document.name + ")");
					fileName = FileBrowser.removeIllegalCharacters(document.filename).toLowerCase(Locale.GERMANY);
				}

				if (fileIndex.contains(fileName)) {
					final Date chDate = new Date(document.chdate * 1000L);
					final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.GERMANY);

					/* 2. Append change date.*/
					document.filename = FileBrowser.appendFilename(document.filename, "_" + format.format(chDate));
					fileName = FileBrowser.removeIllegalCharacters(document.filename).toLowerCase(Locale.GERMANY);

					if (fileIndex.contains(fileName)) {
						/* 3. Append Stud.IP document id. */
						document.filename = FileBrowser.appendFilename(document.filename, "_" + document.document_id);
						fileName = FileBrowser.removeIllegalCharacters(document.filename).toLowerCase(Locale.GERMANY);
					}
				}

				if (LOG.isLoggable(Level.WARNING)) {
					LOG.warning("Resolved to: " + fileName);
				}
			}

			fileIndex.add(fileName);
		}
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
	protected void updateProgressLabel(final String text) {
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