package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.ForbiddenException;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.utils.FileHash;

/**
 * Semester/Course/Folder/Document tree sync.
 * 
 * @author Lennart Glauer
 */
public class TreeSync {
	
	private final File rootDirectory;
	
	private final ExecutorService threadPool;

	public TreeSync(File rootDirectory) {
		if (!rootDirectory.isDirectory()) {
			throw new IllegalStateException("Root directory does not exist!");
		}
		
		this.rootDirectory = rootDirectory;
		
		threadPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	}
	
	public void shutdown() {
		threadPool.shutdown();
	}
	
	/**
	 * Synchronize all documents.
	 * 
	 * @param tree
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized void sync(File tree) throws JsonParseException, JsonMappingException, IOException {
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
				File semesterDirectory = new File(rootDirectory, semester.title);
				
				if (!semesterDirectory.exists() && !semesterDirectory.mkdir()) {
					throw new IllegalStateException("Could not create semester directory!");
				}
				
				for (CourseTreeNode course : semester.courses) {
					File courseDirectory = new File(semesterDirectory, course.title);
					
					if (!courseDirectory.exists() && !courseDirectory.mkdir()) {
						throw new IllegalStateException("Could not create course directory!");
					}
					
					doFolder(phaser, course.root, courseDirectory);
				}
			}
		}
		
		/* Wait until all jobs are done. */
		phaser.arriveAndAwaitAdvance();
		
		System.out.println("Sync done!");
	}
	
	/**
	 * Folder node handler.
	 * 
	 * @param phaser
	 * @param parentNode
	 * @param parentDirectory
	 */
	private void doFolder(Phaser phaser, DocumentFolderTreeNode parentNode, File parentDirectory) {
		/* Traverse folder structure (recursive). */
		for (DocumentFolderTreeNode folder : parentNode.folders) {
			File folderDirectory = new File(parentDirectory, folder.name);
			
			if (!folderDirectory.exists() && !folderDirectory.mkdir()) {
				throw new IllegalStateException("Could not create course directory!");
			}
			
			doFolder(phaser, folder, folderDirectory);
		}

		for (DocumentTreeNode document : parentNode.documents) {
			doDocument(phaser, document, parentDirectory);
		}
	}
	
	/**
	 * Document node handler.
	 * 
	 * @param phaser
	 * @param documentNode
	 * @param parentDirectory
	 */
	private void doDocument(Phaser phaser, DocumentTreeNode documentNode, File parentDirectory) {
		File documentFile = new File(parentDirectory, documentNode.filename);
		
		if (documentFile.exists()) {
			try {
				/* Compare file size and md5 hash. */
				if (documentFile.length() != documentNode.filesize || !FileHash.getMd5(documentFile).equals(documentNode.document_id)) {
					phaser.register();
					
					/* Download modified file. */
					/* TODO: Add option to overwrite or rename file. */
					threadPool.execute(new DownloadDocumentJob(phaser, documentNode, documentFile));
					
					/* Logging. */
					System.out.println(documentNode.name);
				}
				
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		} else {
			phaser.register();
			
			/* Download new file. */
			threadPool.execute(new DownloadDocumentJob(phaser, documentNode, documentFile));
			
			/* Logging. */
			System.out.println(documentNode.name);
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
		 * Document node.
		 */
		private final DocumentTreeNode documentNode;
		
		/**
		 * Document file.
		 */
		private final File documentFile;
		
		public DownloadDocumentJob(Phaser phaser, DocumentTreeNode documentNode, File documentFile) {
			this.phaser = phaser;
			this.documentNode = documentNode;
			this.documentFile = documentFile;
		}

		@Override
		public void run() {
			try {
				RestApi.downloadDocumentById(documentNode.document_id, documentFile);
				
				/* Logging. */
				System.out.println(documentNode.name + " done!");
				
			} catch (UnauthorizedException e) {
				e.printStackTrace();
			} catch (ForbiddenException e) {
				e.printStackTrace();
			} catch (NotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				phaser.arrive();
			}
		}
		
	}
}
