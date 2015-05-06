package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.elanev.studip.android.app.backend.datamodel.*;
import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.*;

/**
 * Semester/Course/Folder/Document tree builder.
 * 
 * @author Lennart Glauer
 */
public class TreeBuilder implements AutoCloseable {

	/**
	 * Thread pool.
	 */
	protected final ExecutorService threadPool;

	/**
	 * Gui progress indicator.
	 */
	private ProgressIndicator progressIndicator;

	private Label progressLabel;

	protected TreeBuilder() {
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@Override
	public void close() {
		threadPool.shutdownNow();
	}

	/**
	 * Build the semester/course/folder/document tree and store it in json format.
	 * 
	 * This method always creates a new tree!
	 * 
	 * @param tree
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized int build(File tree) throws JsonGenerationException, JsonMappingException, IOException {
		/* Create empty root node. */
		SemestersTreeNode rootNode = new SemestersTreeNode();
		
		/* A phaser is actually a up and down latch, it's used to wait until all jobs are done. */
		Phaser phaser = new Phaser(2); /* = self + first job. */
		
		/* Build tree with multiple threads. */
		threadPool.execute(new BuildSemestersJob(phaser, rootNode));
		
		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();
		
		/* Serialize the tree to json and store it in the tree file. */
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(tree, rootNode);
		
		System.out.println("Build done!");
		return phaser.getRegisteredParties() - 1;
	}
	
	/**
	 * Update existing semester/course/folder/document tree.
	 * 
	 * @param tree
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized int update(File tree, boolean doAllSemesters) throws JsonGenerationException, JsonMappingException, IOException {
		/* Read existing tree. */
		ObjectMapper mapper = new ObjectMapper();
		SemestersTreeNode rootNode = mapper.readValue(tree, SemestersTreeNode.class);

		/* A phaser is actually a up and down latch, it's used to wait until all jobs are done. */
		Phaser phaser = new Phaser(1); /* = self. */

		/* Current unix timestamp. */
		long now = System.currentTimeMillis() / 1000L;

		/* Update tree with multiple threads. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			/* If doAllSemesters is false we will only update the current semester. */
			if (doAllSemesters || (now > semester.begin && now < semester.end)) {
				for (CourseTreeNode course : semester.courses) {
					/* Cache requests for 10min. */
					if (now - course.update_time > 10 * 60) {
						course.update_time = now;

						phaser.register();

						// TODO
						threadPool.execute(new BuildDocumentsJob(phaser, course, course.root = new DocumentFolderTreeNode()));
						//threadPool.execute(new UpdateDocumentsJob(phaser, course));
					}
				}
			}
		}

		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();

		/* Serialize the tree to json and store it in the tree file. */
		mapper.writeValue(tree, rootNode);

		System.out.println("Update done!");
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
		
		public BuildSemestersJob(Phaser phaser, SemestersTreeNode rootNode) {
			this.phaser = phaser;
			this.rootNode = rootNode;
		}

		@Override
		public void run() {
			try {
				SemesterTreeNode semesterNode;
				
				/* Get all visible semesters. */
				Semesters semesters = RestApi.getAllSemesters();
				
				phaser.bulkRegister(semesters.semesters.size());
				
				for (Semester semester : semesters.semesters) {
					rootNode.semesters.add(semesterNode = new SemesterTreeNode(semester));
					
					/* Add update courses job. */
					threadPool.execute(new BuildCoursesJob(phaser, semesterNode));

					System.out.println(semesterNode.title);
				}

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				OAuth.getInstance().removeAccessToken();
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				//updateProgress(phaser);
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
		
		public BuildCoursesJob(Phaser phaser, SemesterTreeNode semesterNode) {
			this.phaser = phaser;
			this.semesterNode = semesterNode;
		}

		@Override
		public void run() {
			try {
				CourseTreeNode courseNode;

				/* Get subscribed courses. */
				Courses courses = RestApi.getAllCoursesBySemesterId(semesterNode.semester_id);

				phaser.bulkRegister(courses.courses.size());
				
				for (Course course : courses.courses) {
					semesterNode.courses.add(courseNode = new CourseTreeNode(course));
					
					/* Add update files job. */
					threadPool.execute(new BuildDocumentsJob(phaser, courseNode, courseNode.root));
					
					System.out.println(courseNode.title);
				}
				
			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				OAuth.getInstance().removeAccessToken();
			} catch (NotFoundException e) {
				/* Course does not exist. */
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				updateProgress(phaser, semesterNode.title);
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
		
		public BuildDocumentsJob(Phaser phaser, CourseTreeNode courseNode, DocumentFolderTreeNode parentNode) {
			this.phaser = phaser;
			this.courseNode = courseNode;
			this.parentNode = parentNode;
		}

		@Override
		public void run() {
			try {
				DocumentFolderTreeNode folderNode;
				DocumentTreeNode documentNode;

				HashSet<String> fileNames = new HashSet<String>();

				/* Get course folder content. */
				DocumentFolders folders = RestApi.getAllDocumentsByRangeAndFolderId(courseNode.course_id, parentNode.folder_id);

				phaser.bulkRegister(folders.folders.size());

				/* Folders. */
				for (DocumentFolder folder : folders.folders) {
					/*
					 * Maybe the folder contains multiple folders with same name,
					 * we need to assign a unique name in this case.
					 */
					if (fileNames.contains(folder.name)) {
						System.out.println("Duplicate foldername: " + folder.name);
						folder.name = appendFilename(folder.name, "_" + folder.folder_id);
					}

					parentNode.folders.add(folderNode = new DocumentFolderTreeNode(folder));
					fileNames.add(folderNode.name);

					/* Add update files job (recursive). */
					threadPool.execute(new BuildDocumentsJob(phaser, courseNode, folderNode));

					System.out.println(folderNode.name);
				}

				/* Documents. */
				for (Document document : folders.documents) {
					/*
					 * Maybe the folder contains multiple documents with same filename,
					 * we need to assign a unique filename in this case.
					 */
					if (fileNames.contains(document.filename)) {
						System.out.println("Duplicate filename: " + document.filename);
						document.filename = appendFilename(document.filename, "_" + document.document_id);
					}

					parentNode.documents.add(documentNode = new DocumentTreeNode(document));
					fileNames.add(document.filename);

					System.out.println(documentNode.name);
				}

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				OAuth.getInstance().removeAccessToken();
			} catch (ForbiddenException | NotFoundException e) {
				/*
				 * User does not have the required permissions
				 * or folder does not exist.
				 */
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				updateProgress(phaser, courseNode.title);
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
		 * Course node.
		 */
		private final CourseTreeNode courseNode;
		
		public UpdateDocumentsJob(Phaser phaser, CourseTreeNode courseNode) {
			this.phaser = phaser;
			this.courseNode = courseNode;
		}

		/**
		 * Build folder index, so we can access the folder nodes in constant time.
		 * 
		 * @param parentFolder
		 * @return
		 */
		private HashMap<String, DocumentFolderTreeNode> buildFolderIndex(DocumentFolderTreeNode parentFolder) {
			HashMap<String, DocumentFolderTreeNode> folderIndex = new HashMap<String, DocumentFolderTreeNode>();
			folderIndex.put(parentFolder.folder_id, parentFolder);

			for (DocumentFolderTreeNode folder : parentFolder.folders) {
				folderIndex.putAll(buildFolderIndex(folder));
			}

			return folderIndex;
		}

		/**
		 * Remove document node if it exists.
		 * 
		 * @param folder
		 * @param document
		 * @return
		 */
		private boolean removeDocument(DocumentFolderTreeNode folder, Document document) {
			// TODO: Use iterator?
			for (DocumentTreeNode d : folder.documents) {
				if (d.document_id.equals(document.document_id)) {
					folder.documents.remove(d);
					return true;
				}
			}

			return false;
		}

		/**
		 * Test if the folder contains multiple documents with same filename.
		 * 
		 * @param folder
		 * @param document
		 * @return
		 */
		private boolean hasDuplicates(DocumentFolderTreeNode folder, Document document) {
			for (DocumentTreeNode d : folder.documents) {
				if (d.filename.equals(document.filename)
						&& !d.document_id.equals(document.document_id)) {
					return true;
				}
			}

			return false;
		}

		@Override
		public void run() {
			try {
				DocumentFolderTreeNode folderNode;
				DocumentTreeNode documentNode;

				/* Get all course documents with newer change date than course update time. */
				Documents newDocuments = RestApi.getNewDocumentsByCourseId(courseNode.course_id, courseNode.update_time);
				/* Build a folder index for this course, so we can easily access the folders. */
				HashMap<String, DocumentFolderTreeNode> folderIndex = buildFolderIndex(courseNode.root);

				for (Document document : newDocuments.documents) {
					folderNode = folderIndex.get(document.folder_id);
					if (folderNode == null) {
						/* Folder does not exist locally, we need to re-sync all course folders. */
						phaser.register();

						threadPool.execute(new BuildDocumentsJob(phaser, courseNode, courseNode.root = new DocumentFolderTreeNode()));
						break;
					}

					/*
					 * Maybe the document was updated and the node already exists,
					 * we need to replace the document node (remove + add).
					 */
					removeDocument(folderNode, document);

					/*
					 * Maybe the folder contains multiple documents with same filename,
					 * we need to assign a unique filename in this case.
					 */
					if (hasDuplicates(folderNode, document)) {
						System.out.println("Duplicate filename: " + document.filename);
						document.filename = appendFilename(document.filename, "_" + document.document_id);
					}

					/* Add document to existing folder. */
					folderNode.documents.add(documentNode = new DocumentTreeNode(document));

					System.out.println(documentNode.name);
				}

			} catch (UnauthorizedException e) {
				/* Invalid oauth access token. */
				OAuth.getInstance().removeAccessToken();
			} catch (ForbiddenException | NotFoundException e) {
				/*
				 * User does not have the required permissions
				 * or course does not exist.
				 */
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				updateProgress(phaser, courseNode.title);
			}
		}
	}

	/**
	 * Appends the suffix to the filename (before file extension).
	 * 
	 * @param filename
	 * @param suffix
	 * @return
	 */
	protected String appendFilename(String filename, String suffix) {
		int ext = filename.lastIndexOf(".");
		if (ext == -1) {
			ext = filename.length();
		}

		return filename.substring(0, ext) + suffix + filename.substring(ext);
	}

	/**
	 * Set gui progress indicator.
	 * 
	 * @param progress
	 */
	public void setProgress(ProgressIndicator progress, Label label) {
		progressIndicator = progress;
		progressLabel = label;
	}

	/**
	 * Update gui progress indicator.
	 * 
	 * @param phaser
	 */
	protected void updateProgress(final Phaser phaser, final String text) {
		if (progressIndicator != null) {
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					progressIndicator.setProgress((double) phaser.getArrivedParties() / phaser.getRegisteredParties());
					progressLabel.setText(text);
				}

			});
		}
	}
}