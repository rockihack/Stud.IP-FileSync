package de.uni.hannover.studip.sync;

import java.io.IOException;
import java.util.Scanner;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.exceptions.ForbiddenException;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.FileSync;
import de.uni.hannover.studip.sync.models.OAuth;
import de.uni.hannover.studip.sync.models.RestApi;

public class Application {
	
	private static final OAuth oauth = OAuth.getInstance();

	public static void main(String[] args) {
		
		System.out.println("Starting...");
		
		/*
		oauth.getRequestToken();
		System.out.println(oauth.getAuthUrl());
		
		Scanner sc = new Scanner(System.in);
		String verifier = sc.nextLine();
		
		oauth.getAccessToken(verifier);
		*/
		
		
		oauth.restoreAccessToken();
		
		FileSync sync = new FileSync();
		
		sync.createTree();
		
		/*
		try {
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
			//RestApi.downloadDocumentById("56a5bcc9b93af836f24f98168da27a1b", "<path-to-file>");
			
			RestApi.getAllSemesters();
			
			//RestApi.getSemesterById("");
			
			User user = RestApi.getUser();
			System.out.println(user.username);
			
			//RestApi.getUserById("");
			
		} catch (UnauthorizedException e) {
			e.printStackTrace();
		} catch (ForbiddenException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/
		
	}

}
