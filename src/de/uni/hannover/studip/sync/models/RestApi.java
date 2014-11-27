package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.util.Set;

import org.scribe.model.Verb;

import de.elanev.studip.android.app.backend.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.utils.FileDownload;

public class RestApi {
	
	private static final String BASE_URL = "elearning.uni-hannover.de/plugins.php/restipplugin";
	
	private static final boolean DEBUG = false;

	/**
	 * Liefert Informationen über die in der Instanz unterstützten Routen, ihrer Methoden und den jeweiligen Zugriffsberechtigungen zurück.
	 * 
	 * @return
	 * @throws UnauthorizedException
	 * @throws IOException 
	 */
	public static Discovery discovery() throws UnauthorizedException, IOException {
		Discovery discovery = null;
		
		JacksonRequest<Discovery> request = new JacksonRequest<Discovery>(Verb.GET,
				"https://" + BASE_URL + "/api/discovery", Discovery.class);
		
		switch (request.getCode()) {
		case 200:
			discovery = request.parseResponse(false);

			if (DEBUG) {
				/* A set containing all possible routes. */
				Set<String> routes = discovery.routes.keySet();
				
				/* Log all routes for debugging. */
				for (String route : routes) {
					Route r = discovery.routes.get(route);
					
					System.out.println(route + ":\nGET:" + r.get + ",\tPOST:" + r.post + ",\tPUT:" + r.put + ",\tDELETE:" + r.delete + "\n");
				}
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return discovery;
	}
	
	/**
	 * Liefert die Veranstaltungen zurück, in die der Nutzer eingetragen ist. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Courses getAllCourses() throws UnauthorizedException, IOException {
		Courses courses = null;
		
		JacksonRequest<Courses> request = new JacksonRequest<Courses>(Verb.GET,
				"https://" + BASE_URL + "/api/courses", Courses.class);
		
		switch (request.getCode()) {
		case 200:
			courses = request.parseResponse(false);
			
			if (DEBUG) {
				for (Course course : courses.courses) {
					System.out.println(course.title + "\n" + course.description + "\n");
				}
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return courses;
	}
	
	/**
	 * Liefert alle Semester zurück, in denen der Nutzer in mindestens eine Veranstaltung eingetragen ist.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Courses getAllCoursesBySemesterId(String semesterId) throws UnauthorizedException, NotFoundException, IOException {
		Courses courses = null;
		
		JacksonRequest<Courses> request = new JacksonRequest<Courses>(Verb.GET,
				"https://" + BASE_URL + "/api/courses/semester/" + semesterId, Courses.class);

		switch (request.getCode()) {
		case 200:
			courses = request.parseResponse(false);
			
			if (DEBUG) {
				for (Course course : courses.courses) {
					System.out.println(course.title + "\n" + course.description + "\n");
				}
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return courses;
	}
	
	/**
	 * Liefert die Daten der angegebenen Veranstaltung zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Course getCourseById(String courseId) throws UnauthorizedException, NotFoundException, IOException {
		Course course = null;
		
		JacksonRequest<Course> request = new JacksonRequest<Course>(Verb.GET,
				"https://" + BASE_URL + "/api/courses/" + courseId, Course.class);
		
		switch (request.getCode()) {
		case 200:
			course = request.parseResponse(true);
			
			if (DEBUG) {
				System.out.println(course.title + "\n" + course.description + "\n");
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return course;
	}
	
	/**
	 * Liefert die Dateien und Ordner eines angegebenen Ordners einer Veranstaltung zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static DocumentFolders getAllDocumentsByRangeId(String rangeId, String folderId) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		DocumentFolders documentFolders = null;
		
		JacksonRequest<DocumentFolders> request = new JacksonRequest<DocumentFolders>(Verb.GET,
				"https://" + BASE_URL + "/api/documents/" + rangeId + "/folder" + (folderId.isEmpty() ? "" : "/" + folderId), DocumentFolders.class);

		switch (request.getCode()) {
		case 200:
			documentFolders = request.parseResponse(false);
			
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
			
			break;
			
		case 400: /* Range has no documents. */
			documentFolders = new DocumentFolders();
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
		
		return documentFolders;
	}
	
	/**
	 * Liefert die Daten eines Dokuments zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Document getDocumentById(String documentId) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		Document document = null;
		
		JacksonRequest<Document> request = new JacksonRequest<Document>(Verb.GET,
				"https://" + BASE_URL + "/api/documents/" + documentId, Document.class);
		
		switch (request.getCode()) {
		case 200:
			document = request.parseResponse(false);
			
			if (DEBUG) {
				System.out.println(document.name + "\n");
			}
			
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
		
		return document;
	}
	
	/**
	 * Liefert das Dokument als solches zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static void downloadDocumentById(String documentId, String localPath) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		long startTime = System.currentTimeMillis();
		
		JacksonRequest<Object> request = new JacksonRequest<Object>(Verb.GET,
				"https://" + BASE_URL + "/api/documents/" + documentId + "/download", Object.class);
		
		switch (request.getCode()) {
		case 200:
			FileDownload.get(request.getStream(), localPath);
			
			long endTime = System.currentTimeMillis();
			System.out.println("Downloaded " + localPath + " in " + (endTime - startTime) + "ms");
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
	 * Liefert alle Semester zurück, in denen der Nutzer in mindestens eine Veranstaltung eingetragen ist.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Semesters getAllSemesters() throws UnauthorizedException, IOException {
		Semesters semesters = null;
		
		JacksonRequest<Semesters> request = new JacksonRequest<Semesters>(Verb.GET,
				"https://" + BASE_URL + "/api/courses/semester", Semesters.class);
		
		switch (request.getCode()) {
		case 200:
			semesters = request.parseResponse(false);
			
			if (DEBUG) {
				for (Semester semester : semesters.semesters) {
					System.out.println(semester.title + "\n");
				}
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return semesters;
	}
	
	/**
	 * Liefert die Daten des angegebenen Semesters zurück.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Semester getSemesterById(String semesterId) throws UnauthorizedException, NotFoundException, IOException {
		Semester semester = null;
		
		JacksonRequest<Semester> request = new JacksonRequest<Semester>(Verb.GET,
				"https://" + BASE_URL + "/api/semesters/" + semesterId, Semester.class);
		
		switch (request.getCode()) {
		case 200:
			semester = request.parseResponse(false);
			
			if (DEBUG) {
				System.out.println(semester.title + "\n");
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return semester;
	}
	
	/**
	 * Liefert die Daten des Nutzers mit der angegebenen Id zurück. Ist keine Id angegeben, so werden die Daten des autorisierten Nutzers zurückgegeben.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static User getUser() throws UnauthorizedException, IOException {
		User user = null;
		
		JacksonRequest<User> request = new JacksonRequest<User>(Verb.GET,
				"https://" + BASE_URL + "/api/user", User.class);

		switch (request.getCode()) {
		case 200:
			user = request.parseResponse(true);
			
			if (DEBUG) {
				System.out.println(user.username + "\n");
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return user;
	}
	
	/**
	 * Liefert die Daten des Nutzers mit der angegebenen Id zurück. Ist keine Id angegeben, so werden die Daten des autorisierten Nutzers zurückgegeben.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static User getUserById(String userId) throws UnauthorizedException, NotFoundException, IOException {
		User user = null;
		
		JacksonRequest<User> request = new JacksonRequest<User>(Verb.GET,
				"https://" + BASE_URL + "/api/user/" + userId, User.class);

		switch (request.getCode()) {
		case 200:
			user = request.parseResponse(true);
			
			if (DEBUG) {
				System.out.println(user.username + "\n");
			}
			
			break;
			
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
		
		return user;
	}
}
