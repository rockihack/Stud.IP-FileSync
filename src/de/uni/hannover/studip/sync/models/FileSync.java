package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.elanev.studip.android.app.backend.datamodel.Course;
import de.elanev.studip.android.app.backend.datamodel.Courses;
import de.elanev.studip.android.app.backend.datamodel.Document;
import de.elanev.studip.android.app.backend.datamodel.DocumentFolder;
import de.elanev.studip.android.app.backend.datamodel.DocumentFolders;
import de.elanev.studip.android.app.backend.datamodel.Semester;
import de.elanev.studip.android.app.backend.datamodel.Semesters;
import de.uni.hannover.studip.sync.exceptions.ForbiddenException;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;

public class FileSync {
	
	private static final int MAX_THREADS = 4;
	
	private static final File ROOT_DIR = new File("<path-to-directory>");
	
	ExecutorService threadPool;

	public FileSync() {
		threadPool = Executors.newFixedThreadPool(MAX_THREADS);
	}
	
	public synchronized void createTree() {
		threadPool.execute(new UpdateSemestersJob(ROOT_DIR));
	}
	
	/**
	 * Update semester job.
	 * 
	 * @author Lennart Glauer
	 */
	private class UpdateSemestersJob implements Runnable {
		
		private final File rootDir;
		
		private Semesters semesters;
		
		public UpdateSemestersJob(File rootDir) {
			if (!rootDir.isDirectory()) {
				throw new IllegalArgumentException("Root dir is not a directory!");
			}
			
			if (!rootDir.exists()) {
				throw new IllegalArgumentException("Root dir does not exist!");
			}
			
			this.rootDir = rootDir;
		}

		@Override
		public void run() {
			try {
				semesters = RestApi.getAllSemesters();
				
				File semesterDir;
				
				for (Semester semester : semesters.semesters) {
					/* Replace file separators. */
					semester.title = semester.title.replaceAll("[\\/]+", "-");
					/* Remove illegal chars. */
					semester.title = semester.title.replaceAll("[:]+", "");
					
					/* Create course directory if it doesn't exist. */
					semesterDir = new File(rootDir + File.separator + semester.title);
					if (!semesterDir.exists() && !semesterDir.mkdir()) {
						throw new IllegalStateException("Failed to create: " + semesterDir);
					}
					
					/* Add update courses job. */
					threadPool.execute(new UpdateCoursesJob(semesterDir, semester));
					
					/* Logging. */
					System.out.println(semesterDir);
				}
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Update courses job.
	 * 
	 * @author Lennart Glauer
	 */
	private class UpdateCoursesJob implements Runnable {
		
		private final File rootDir;
		
		private final Semester semester;
		
		public UpdateCoursesJob(File rootDir, Semester semester) {
			if (!rootDir.isDirectory()) {
				throw new IllegalArgumentException("Root dir is not a directory!");
			}
			
			if (!rootDir.exists()) {
				throw new IllegalArgumentException("Root dir does not exist!");
			}
			
			this.rootDir = rootDir;
			
			this.semester = semester;
		}

		@Override
		public void run() {
			try {
				Courses courses = RestApi.getAllCoursesBySemesterId(semester.semester_id);
				
				File courseDir;
				
				for (Course course : courses.courses) {
					/* Replace file separators. */
					course.title = course.title.replaceAll("[\\/]+", "-");
					/* Remove illegal chars. */
					course.title = course.title.replaceAll("[:]+", "");
					
					/* Create course directory if it doesn't exist. */
					courseDir = new File(rootDir + File.separator + course.title);
					if (!courseDir.exists() && !courseDir.mkdir()) {
						throw new IllegalStateException("Failed to create: " + courseDir);
					}
					
					/* Add update files job. */
					threadPool.execute(new UpdateDocumentsJob(courseDir, course.courseId, ""));
					
					/* Logging. */
					System.out.println(courseDir);
				}
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Update files job.
	 * 
	 * @author Lennart Glauer
	 */
	private class UpdateDocumentsJob implements Runnable {
		
		private final File rootDir;
		
		private final String courseId;
		
		private final String folderId;
		
		public UpdateDocumentsJob(File rootDir, String courseId, String folderId) {
			if (!rootDir.isDirectory()) {
				throw new IllegalArgumentException("Root dir is not a directory!");
			}
			
			if (!rootDir.exists()) {
				throw new IllegalArgumentException("Root dir does not exist!");
			}
			
			this.rootDir = rootDir;
			
			this.courseId = courseId;
			
			this.folderId = folderId;
		}

		@Override
		public void run() {
			try {
				DocumentFolders documentFolders = RestApi.getAllDocumentsByRangeId(courseId, folderId);
				
				File documentDir;
				
				/* Folders. */
				for (DocumentFolder folder : documentFolders.folders) {
					/* Replace file separators. */
					folder.name = folder.name.replaceAll("[\\/]+", "-");
					/* Remove illegal chars. */
					folder.name = folder.name.replaceAll("[:]+", "");
					
					/* Create course directory if it doesn't exist. */
					documentDir = new File(rootDir + File.separator + folder.name);
					if (!documentDir.exists() && !documentDir.mkdir()) {
						throw new IllegalStateException("Failed to create: " + documentDir);
					}
					
					/* Add update files job. */
					threadPool.execute(new UpdateDocumentsJob(documentDir, courseId, folder.folder_id));
					
					/* Logging. */
					System.out.println(documentDir);
				}
				
				/* Documents. */
				for (Document document : documentFolders.documents) {
					/* Replace file separators. */
					document.name = document.name.replaceAll("[\\/]+", "-");
					/* Remove illegal chars. */
					document.name = document.name.replaceAll("[:]+", "");
					
					/* Download document if it doesn't exist. */
					documentDir = new File(rootDir + File.separator + document.name);
					if (!documentDir.exists()) {
						/* Add download document job. */
						threadPool.execute(new DownloadDocumentJob(rootDir, document));
					}
				}
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (ForbiddenException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Download file job.
	 * 
	 * @author Lennart Glauer
	 */
	private class DownloadDocumentJob implements Runnable {
		
		private final File rootDir;
		
		private final Document document;
		
		public DownloadDocumentJob(File rootDir, Document document) {
			if (!rootDir.isDirectory()) {
				throw new IllegalArgumentException("Root dir is not a directory!");
			}
			
			if (!rootDir.exists()) {
				throw new IllegalArgumentException("Root dir does not exist!");
			}
			
			this.rootDir = rootDir;
			
			this.document = document;
		}

		@Override
		public void run() {
			try {
				/* Local document path. */
				String documentPath = rootDir + File.separator + document.filename;
				
				/* Logging. */
				System.out.println(documentPath);
				
				RestApi.downloadDocumentById(document.document_id, documentPath);
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (ForbiddenException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
}
