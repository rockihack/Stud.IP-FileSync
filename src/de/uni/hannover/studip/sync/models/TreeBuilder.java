package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

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
public class TreeBuilder {

	private final ExecutorService threadPool;
	
	public TreeBuilder() {
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	public void shutdown() {
		threadPool.shutdown();
	}
	
	/**
	 * Build the semester/course/folder/document tree and store it in json format.
	 * 
	 * @param output
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized void build(File tree) throws JsonGenerationException, JsonMappingException, IOException {
		/* Create empty root node. */
		SemestersTreeNode rootNode = new SemestersTreeNode();
		
		/* A phaser is actually a up and down latch, it's used to wait until all jobs are done. */
		Phaser phaser = new Phaser();
		
		/* Register self and first job. */
		phaser.bulkRegister(2);
		
		/* Build tree with multiple threads. */
		threadPool.execute(new UpdateSemestersJob(phaser, rootNode));
		
		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();
		
		/* Serialize the tree to json. */
		ObjectMapper mapper = new ObjectMapper();
		
		mapper.writeValue(tree, rootNode);
		
		System.out.println("Build done!");
	}
	
	/**
	 * Update the semester/course/folder/document tree.
	 * 
	 * @param input
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized void update(File tree) throws JsonGenerationException, JsonMappingException, IOException {
		/* Read existing tree. */
		ObjectMapper mapper = new ObjectMapper();

		SemestersTreeNode rootNode = mapper.readValue(tree, SemestersTreeNode.class);
		
		/* A phaser is actually a up and down latch, it's used to wait until all jobs are done. */
		Phaser phaser = new Phaser();
		
		/* Register self. */
		phaser.register();
		
		/* Current unix timestamp. */
		long now = System.currentTimeMillis() / 1000L;
		
		/* Update tree with multiple threads. */
		for (SemesterTreeNode semester : rootNode.semesters) {
			/* Update only current semester. */
			if (now > semester.begin && now < semester.end) {
				phaser.bulkRegister(semester.courses.size());
				
				for (CourseTreeNode course : semester.courses) {
					threadPool.execute(new UpdateDocumentsJob(phaser, course, course.root));
				}
			}
		}
		
		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();
		
		/* Serialize the tree to json. */
		mapper.writeValue(tree, rootNode);
		
		System.out.println("Update done!");
	}
	
	/**
	 * Remove/replace illegal chars from path/file name.
	 * 
	 * @param file
	 * @return
	 */
	private String removeIllegalCharacters(String file) {
		/* Replace separators. */
		file = file.replaceAll("[\\/]+", "-");
		/* Remove other illegal chars. */
		return file.replaceAll("[<>:\"|?*]+", "");
	}
	
	/**
	 * Update semesters job.
	 * 
	 * @author Lennart Glauer
	 */
	private class UpdateSemestersJob implements Runnable {
		
		/**
		 * Phaser.
		 */
		private final Phaser phaser;
		
		/**
		 * Tree root node.
		 */
		private final SemestersTreeNode rootNode;
		
		public UpdateSemestersJob(Phaser phaser, SemestersTreeNode rootNode) {
			this.phaser = phaser;
			this.rootNode = rootNode;
		}

		@Override
		public void run() {
			try {
				SemesterTreeNode semesterNode;
				
				Semesters semesters = RestApi.getAllSemesters();
				
				/* Register new jobs. */
				phaser.bulkRegister(semesters.semesters.size());
				
				for (Semester semester : semesters.semesters) {
					semester.title = removeIllegalCharacters(semester.title);
					
					/* Add semester tree node. */
					rootNode.semesters.add(semesterNode = new SemesterTreeNode(semester));
					
					/* Add update courses job. */
					threadPool.execute(new UpdateCoursesJob(phaser, semesterNode));

					/* Logging. */
					System.out.println(semesterNode.title);
				}

			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				/* Job done. */
				phaser.arrive();
			}
		}
		
	}
	
	/**
	 * Update courses job.
	 * 
	 * @author Lennart Glauer
	 */
	private class UpdateCoursesJob implements Runnable {
		
		/**
		 * Phaser.
		 */
		private final Phaser phaser;
		
		/**
		 * Semester node.
		 */
		private final SemesterTreeNode semesterNode;
		
		public UpdateCoursesJob(Phaser phaser, SemesterTreeNode semesterNode) {
			this.phaser = phaser;
			this.semesterNode = semesterNode;
		}

		@Override
		public void run() {
			try {
				CourseTreeNode courseNode;
				
				Courses courses = RestApi.getAllCoursesBySemesterId(semesterNode.semester_id);

				/* Register new jobs. */
				phaser.bulkRegister(courses.courses.size());
				
				for (Course course : courses.courses) {
					course.title = removeIllegalCharacters(course.title);

					/* Add course tree node. */
					semesterNode.courses.add(courseNode = new CourseTreeNode(course));
					
					/* Add update files job. */
					threadPool.execute(new UpdateDocumentsJob(phaser, courseNode, courseNode.root));
					
					/* Logging. */
					System.out.println(courseNode.title);
				}
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				/* Job done. */
				phaser.arrive();
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
		
		/**
		 * Parent folder node.
		 */
		private final DocumentFolderTreeNode parentNode;
		
		public UpdateDocumentsJob(Phaser phaser, CourseTreeNode courseNode, DocumentFolderTreeNode parentNode) {
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
				
				/* Register new jobs. */
				phaser.bulkRegister(folders.folders.size());
				
				/* Clear before update. */
				parentNode.folders.clear();
				parentNode.documents.clear();
				
				/* Folders. */
				for (DocumentFolder folder : folders.folders) {
					folder.name = removeIllegalCharacters(folder.name);
					
					/* Add folder tree node. */
					parentNode.folders.add(folderNode = new DocumentFolderTreeNode(folder));
					
					/* Add update files job (recursive). */
					threadPool.execute(new UpdateDocumentsJob(phaser, courseNode, folderNode));
					
					/* Logging. */
					System.out.println(folderNode.name);
				}

				/* Documents. */
				for (Document document : folders.documents) {
					document.filename = removeIllegalCharacters(document.filename);
					
					/* Add document tree node. */
					parentNode.documents.add(documentNode = new DocumentTreeNode(document));
					
					/* Logging. */
					System.out.println(documentNode.name);
				}
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (ForbiddenException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				/* Job done. */
				phaser.arrive();
			}
		}
		
	}

}
