package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.scribe.model.Verb;

import de.elanev.studip.android.app.backend.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileDownload;

/**
 * 
 * @author Lennart Glauer
 * 
 * @see http://studip.github.io/studip-rest.ip/
 *
 */
public class RestApi {
	
	/**
	 * Debug flag.
	 */
	private static final boolean DEBUG = false;

	/**
	 * Liefert Informationen �ber die in der Instanz unterst�tzten Routen, ihrer Methoden und den jeweiligen Zugriffsberechtigungen zur�ck.
	 * 
	 * @return
	 * @throws UnauthorizedException
	 * @throws IOException 
	 */
	public static Discovery discovery() throws UnauthorizedException, IOException {
		JacksonRequest<Discovery> request = new JacksonRequest<Discovery>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/discovery", Discovery.class);
		
		switch (request.getCode()) {
		case 200:
			Discovery discovery = request.parseResponse(false);

			if (DEBUG) {
				/* A set containing all possible routes. */
				Set<String> routes = discovery.routes.keySet();
				
				/* Log all routes for debugging. */
				for (String route : routes) {
					Route r = discovery.routes.get(route);
					
					System.out.println(route + ":\nGET:" + r.get + ",\tPOST:" + r.post + ",\tPUT:" + r.put + ",\tDELETE:" + r.delete + "\n");
				}
			}
			
			return discovery;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Veranstaltungen zur�ck, in die der Nutzer eingetragen ist. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Courses getAllCourses() throws UnauthorizedException, IOException {
		JacksonRequest<Courses> request = new JacksonRequest<Courses>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses", Courses.class);
		
		switch (request.getCode()) {
		case 200:
			Courses courses = request.parseResponse(false);
			
			if (DEBUG) {
				for (Course course : courses.courses) {
					System.out.println(course.title + "\n" + course.description + "\n");
				}
			}
			
			return courses;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert alle Semester zur�ck, in denen der Nutzer in mindestens eine Veranstaltung eingetragen ist.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Courses getAllCoursesBySemesterId(String semesterId) throws UnauthorizedException, NotFoundException, IOException {
		if (!semesterId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid semester id!");
		}
		
		JacksonRequest<Courses> request = new JacksonRequest<Courses>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/semester/" + semesterId, Courses.class);

		switch (request.getCode()) {
		case 200:
			Courses courses = request.parseResponse(false);
			
			if (DEBUG) {
				for (Course course : courses.courses) {
					System.out.println(course.title + "\n" + course.description + "\n");
				}
			}
			
			return courses;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Daten der angegebenen Veranstaltung zur�ck. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Course getCourseById(String courseId) throws UnauthorizedException, NotFoundException, IOException {
		if (!courseId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid course id!");
		}

		JacksonRequest<Course> request = new JacksonRequest<Course>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/" + courseId, Course.class);
		
		switch (request.getCode()) {
		case 200:
			Course course = request.parseResponse(true);
			
			if (DEBUG) {
				System.out.println(course.title + "\n" + course.description + "\n");
			}
			
			return course;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die neuen Dateien einer Veranstaltung zur�ck. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Documents getNewDocumentsByCourseId(String courseId, long timestamp) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!courseId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid range id!");
		}
		
		JacksonRequest<Documents> request = new JacksonRequest<Documents>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + courseId + "/new/" + timestamp, Documents.class);

		switch (request.getCode()) {
		case 200:
			Documents newDocuments = request.parseResponse(false);
			
			if (DEBUG) {
				System.out.println("Number of new documents: " + newDocuments.documents.size());
				for (Document document : newDocuments.documents) {
					System.out.println(document.name + "\n" + document.description + "\n DocumentId: " + document.document_id + "\n");
				}
			}
			
			return newDocuments;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 403:
			throw new ForbiddenException("Forbidden!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Dateien und Ordner eines angegebenen Ordners einer Veranstaltung zur�ck. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static DocumentFolders getAllDocumentsByRangeAndFolderId(String rangeId, String folderId) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!rangeId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid range id!");
		}
		
		if (folderId != null && !folderId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid folder id!");
		}
		
		JacksonRequest<DocumentFolders> request = new JacksonRequest<DocumentFolders>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + rangeId + "/folder" + (folderId == null ? "" : "/" + folderId), DocumentFolders.class);

		switch (request.getCode()) {
		case 200:
			DocumentFolders documentFolders = request.parseResponse(false);
			
			if (DEBUG) {
				System.out.println("Number of subfolders: " + documentFolders.folders.size());
				for (DocumentFolder folder : documentFolders.folders) {
					System.out.println(folder.name + "\n" + folder.description + "\n FolderId: " + folder.folder_id + "\n");
				}
				
				System.out.println("Number of documents: " + documentFolders.documents.size());
				for (Document document : documentFolders.documents) {
					System.out.println(document.name + "\n" + document.description + "\n DocumentId: " + document.document_id + "\n");
				}
			}
			
			return documentFolders;
		case 400: /* Range has no documents. */
			return new DocumentFolders();
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 403:
			throw new ForbiddenException("Forbidden!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Daten eines Dokuments zur�ck. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Document getDocumentById(String documentId) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!documentId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid document id!");
		}

		JacksonRequest<Document> request = new JacksonRequest<Document>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + documentId, Document.class);
		
		switch (request.getCode()) {
		case 200:
			Document document = request.parseResponse(false);
			
			if (DEBUG) {
				System.out.println(document.name + "\n");
			}
			
			return document;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 403:
			throw new ForbiddenException("Forbidden!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert das Dokument als solches zur�ck. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static void downloadDocumentById(String documentId, File documentFile) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!documentId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid document id!");
		}

		JacksonRequest<Object> request = new JacksonRequest<Object>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + documentId + "/download", Object.class);

		switch (request.getCode()) {
		case 200:
			FileDownload.get(request.getStream(), documentFile);

			break;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 403:
			throw new ForbiddenException("Forbidden!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert alle Semester zur�ck, in denen der Nutzer in mindestens eine Veranstaltung eingetragen ist.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Semesters getAllSemesters() throws UnauthorizedException, IOException {
		JacksonRequest<Semesters> request = new JacksonRequest<Semesters>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/semester", Semesters.class);
		
		switch (request.getCode()) {
		case 200:
			Semesters semesters = request.parseResponse(false);
			
			if (DEBUG) {
				for (Semester semester : semesters.semesters) {
					System.out.println(semester.title + "\n");
				}
			}
			
			return semesters;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Daten des angegebenen Semesters zur�ck.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Semester getSemesterById(String semesterId) throws UnauthorizedException, NotFoundException, IOException {
		if (!semesterId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid semester id!");
		}

		JacksonRequest<Semester> request = new JacksonRequest<Semester>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/semesters/" + semesterId, Semester.class);
		
		switch (request.getCode()) {
		case 200:
			Semester semester = request.parseResponse(false);
			
			if (DEBUG) {
				System.out.println(semester.title + "\n");
			}
			
			return semester;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Daten des Nutzers mit der angegebenen Id zur�ck. Ist keine Id angegeben, so werden die Daten des autorisierten Nutzers zur�ckgegeben.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static User getUserById(String userId) throws UnauthorizedException, NotFoundException, IOException {
		if (userId != null && !userId.matches("^[a-z0-9]{32}$")) {
			throw new IllegalArgumentException("Invalid user id!");
		}

		JacksonRequest<User> request = new JacksonRequest<User>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/user" + (userId == null ? "" : "/" + userId), User.class);

		switch (request.getCode()) {
		case 200:
			User user = request.parseResponse(true);
			
			if (DEBUG) {
				System.out.println(user.username + "\n");
			}
			
			return user;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
	
	/**
	 * Liefert die Aktivit�ten im Umfeld des autorisierten Nutzers zur�ck.
	 * 
	 * @notice Beta! Funktioniert nicht mit jeder Version.
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Activities getActivities() throws UnauthorizedException, IOException {
		JacksonRequest<Activities> request = new JacksonRequest<Activities>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/activities", Activities.class);

		switch (request.getCode()) {
		case 200:
			Activities activities = request.parseResponse(true);
			
			if (DEBUG) {
				for (Activity activity : activities.activities) {
					System.out.println("[" + activity.category + "] " + activity.title + " - " + activity.summary + "\n");
				}
			}
			
			return activities;
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
}
