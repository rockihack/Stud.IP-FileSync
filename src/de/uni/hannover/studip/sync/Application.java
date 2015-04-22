package de.uni.hannover.studip.sync;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.models.*;

public class Application {
	
	public static void main(String[] args) {

		/* Create and show GUI. */
//		SwingUtilities.invokeLater(new Runnable(){
//
//			@Override
//			public void run() {
//				/* Initialize the main frame. */
//				MainFrame.getInstance();
//			}
//			
//		});
		
		
		/*OAuth oauth = OAuth.getInstance();
		
		oauth.restoreAccessToken();
		
		try {
			
			//RestApi.downloadDocumentById("b0b0c84a6bfcd380ab25b48d5240af01", "C:\\Users\\rocki\\Documents\\test.pdf");
			
			RestApi.downloadDocumentById("07047add360daa957f03a5263c295fac", "C:\\Users\\rocki\\Documents\\test.txt");
			
		} catch (UnauthorizedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ForbiddenException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		
		OAuth oauth = OAuth.getInstance();
		
		oauth.restoreAccessToken();
		
		TreeBuilder tree = new TreeBuilder();
		TreeSync sync = new TreeSync(new File("C:\\Users\\rocki\\Documents\\FileSync"));
		
		try {
			File treeFile = Config.getInstance().openTreeFile();
			
			//tree.build(treeFile);
			
			//tree.update(treeFile, false);

			sync.sync(treeFile, false);
			
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tree.shutdown();
		sync.shutdown();
		
		
		
		/*OAuth oauth = OAuth.getInstance();
		
		oauth.restoreAccessToken();
		
		FileSync sync = new FileSync();
		
		sync.createTree();*/
		
		
		
		
		
		
		
		
		/*oauth.getRequestToken();
		System.out.println(oauth.getAuthUrl());
		
		Scanner sc = new Scanner(System.in);
		String verifier = sc.nextLine();
		
		oauth.getAccessToken(verifier);*/
		
		/*try {
			RestApi.discovery();
			
			RestApi.getAllCourses();
			
			//RestApi.getAllCoursesBySemesterId("");
			
			// Übung: Übung zu Mathematik I für Ingenieure, WiSe 2013.
			RestApi.getCourseById("d6c17ec33ccabf86b52863ce61e7e852");

			// Übung: Übung zu Mathematik I für Ingenieure, WiSe 2013, Übungsblätter.
			RestApi.getAllDocumentsByRangeId("d6c17ec33ccabf86b52863ce61e7e852", "b8f94a51d86fcc5f980e61c9a0fe46a8");
			
			// Übung: Übung zu Mathematik I für Ingenieure, WiSe 2013, Übungsblätter, ueb_1.pdf.
			//RestApi.getDocumentById("56a5bcc9b93af836f24f98168da27a1b");
			
			// Übung: Übung zu Mathematik I für Ingenieure, WiSe 2013, Übungsblätter, ueb_1.pdf.
			//RestApi.downloadDocumentById("56a5bcc9b93af836f24f98168da27a1b", "C:\\Users\\rocki\\Documents\\ueb_1.pdf");
			
			RestApi.getAllSemesters();
			
			//RestApi.getSemesterById("");
			
			User user = RestApi.getUserById("");
			System.out.println(user.username);
			
		} catch (UnauthorizedException e) {
			e.printStackTrace();
		} catch (ForbiddenException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
		
	}

}
