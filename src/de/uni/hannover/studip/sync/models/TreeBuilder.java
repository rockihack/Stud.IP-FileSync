package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import javafx.application.Platform;
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

	private ProgressIndicator progressIndicator;

	protected TreeBuilder() {
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}

	@Override
	public void close() {
		threadPool.shutdown();
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
		
		/* Serialize the tree to json. */
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
		long cache_time = now; //now - (10*60);
		
		/* Update tree with multiple threads. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			// If doAllSemesters is false we will only update the current semester.
			if (doAllSemesters || (now > semester.begin && now < semester.end)) {
				for (CourseTreeNode course : semester.courses) {
					if (course.update_time < cache_time) {
						course.update_time = now;
					
						phaser.register();
						
						threadPool.execute(new UpdateDocumentsJob(phaser, course));
					}
				}
			}
		}
		
		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();
		
		/* Serialize the tree to json. */
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
				
				Semesters semesters = RestApi.getAllSemesters();
				
				phaser.bulkRegister(semesters.semesters.size());
				
				for (Semester semester : semesters.semesters) {
					rootNode.semesters.add(semesterNode = new SemesterTreeNode(semester));
					
					/* Add update courses job. */
					threadPool.execute(new BuildCoursesJob(phaser, semesterNode));

					System.out.println(semesterNode.title);
				}

			} catch (UnauthorizedException e) {
				// Invalid oauth access token.
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
				
				Courses courses = RestApi.getAllCoursesBySemesterId(semesterNode.semester_id);

				phaser.bulkRegister(courses.courses.size());
				
				for (Course course : courses.courses) {
					semesterNode.courses.add(courseNode = new CourseTreeNode(course));
					
					/* Add update files job. */
					threadPool.execute(new BuildDocumentsJob(phaser, courseNode, courseNode.root));
					
					System.out.println(courseNode.title);
				}
				
			} catch (UnauthorizedException e) {
				// Invalid oauth access token.
				OAuth.getInstance().removeAccessToken();
			} catch (NotFoundException e) {
				// Course does not exist.
				// TODO: Remove course from tree file.
				throw new UnsupportedOperationException(e);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				updateProgress(phaser);
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
				
				DocumentFolders folders = RestApi.getAllDocumentsByRangeAndFolderId(courseNode.course_id, parentNode.folder_id);

				phaser.bulkRegister(folders.folders.size());
				
				/* Folders. */
				for (DocumentFolder folder : folders.folders) {
					parentNode.folders.add(folderNode = new DocumentFolderTreeNode(folder));
					
					/* Add update files job (recursive). */
					threadPool.execute(new BuildDocumentsJob(phaser, courseNode, folderNode));
					
					System.out.println(folderNode.name);
				}

				/* Documents. */
				for (Document document : folders.documents) {
					parentNode.documents.add(documentNode = new DocumentTreeNode(document));
					
					System.out.println(documentNode.name);
				}
				
			} catch (UnauthorizedException e) {
				// Invalid oauth access token.
				OAuth.getInstance().removeAccessToken();
			} catch (ForbiddenException | NotFoundException e) {
				// User does not have the required permissions
				// or document does not exist.
				// TODO: Remove document from tree file.
				throw new UnsupportedOperationException(e);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				updateProgress(phaser);
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

		@Override
		public void run() {
			try {
				DocumentFolderTreeNode folderNode;
				Documents newDocuments = RestApi.getNewDocumentsByCourseId(courseNode.course_id, courseNode.update_time);
				HashMap<String, DocumentFolderTreeNode> folderIndex = buildFolderIndex(courseNode.root);

				for (Document document : newDocuments.documents) {
					folderNode = folderIndex.get(document.folder_id);
					if (folderNode == null) {
						// Folder does not exist, we need to resync the folders.
						phaser.register();
						threadPool.execute(new BuildDocumentsJob(phaser, courseNode, courseNode.root = new DocumentFolderTreeNode()));
						break;
					}

					// Add document to existing folder.
					folderNode.documents.add(new DocumentTreeNode(document));
				}

			} catch (UnauthorizedException e) {
				// Invalid oauth access token.
				OAuth.getInstance().removeAccessToken();
			} catch (ForbiddenException | NotFoundException e) {
				// User does not have the required permissions
				// or document does not exist.
				// TODO: Remove document from tree file.
				throw new UnsupportedOperationException(e);
			} catch (IOException e) {
				throw new IllegalStateException(e);
			} finally {
				/* Job done. */
				phaser.arrive();
				updateProgress(phaser);
			}
		}
	}

	public void setProgress(ProgressIndicator progress) {
		progressIndicator = progress;
	}

	protected void updateProgress(Phaser phaser) {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				progressIndicator.setProgress((double) phaser.getArrivedParties() / phaser.getRegisteredParties());
			}

		});
	}
}