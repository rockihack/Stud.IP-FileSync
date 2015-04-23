package de.uni.hannover.studip.sync;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;
import de.uni.hannover.studip.sync.models.*;

public class Application {
	
	public static void main(String[] args) {
		
		/**
		 * Zuerst benötigt man einen OAuth Access Token.
		 *
		 * Vorbereitung:
		 *   - In der Datei StudIPApiProvider Studip App Key und Secret einfügen
		 *   - Pfad zum Root Directory unten ändern
		 *   - OAuth Code einkommentieren und starten...
		 *
		 * 1) Die Url in der Konsole aufrufen
		 * 2) Einloggen
		 * 3) App bestätigen
		 * 4) oauth_verifier aus der Url kopieren
		 * 5) oauth_verifier in der Konsole einfügen und mit Enter bestätigen
		 *
		 */
		
		OAuth oauth = OAuth.getInstance();
		
		/*
		oauth.getRequestToken();
		System.out.println(oauth.getAuthUrl());
		
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		String verifier = sc.nextLine();

		try {
			oauth.getAccessToken(verifier);

		} catch (UnauthorizedException e) {
			e.printStackTrace();
		} catch (NotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		*/

		oauth.restoreAccessToken();
		
		TreeSync tree = new TreeSync(new File("C:\\Users\\rocki\\Documents\\FileSync"));
		
		try {
			File treeFile = Config.getInstance().openTreeFile();
			
			tree.build(treeFile);

			tree.sync(treeFile, false);
			
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tree.shutdown();
	}
}
