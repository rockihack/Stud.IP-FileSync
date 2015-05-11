package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Logger;

import org.scribe.model.Verb;

import de.elanev.studip.android.app.backend.datamodel.*;
import de.uni.hannover.studip.sync.exceptions.*;
import de.uni.hannover.studip.sync.oauth.StudIPApiProvider;
import de.uni.hannover.studip.sync.utils.FileDownload;

/**
 * Rest.Api utility class.
 * 
 * @author Lennart Glauer
 * 
 * @see http://studip.github.io/studip-rest.ip/
 *
 */
public final class RestApi {

	/**
	 * Logger instance.
	 */
	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * Debug flag.
	 */
	private static final boolean DEBUG = false;

	/**
	 * Regex for studip id (MD5) validation.
	 */
	private static final String STUDIP_ID_REGEX = "^[a-z0-9]{32}$";

	private RestApi() {
		// Utility class.
	}

	/**
	 * Liefert Informationen über die in der Instanz unterstützten Routen, ihrer Methoden und den jeweiligen Zugriffsberechtigungen zurück.
	 * 
	 * @return
	 * @throws UnauthorizedException
	 * @throws IOException 
	 */
	public static Discovery discovery() throws UnauthorizedException, IOException {
		final JacksonRequest<Discovery> request = new JacksonRequest<Discovery>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/discovery", Discovery.class);
		
		switch (request.getCode()) {
		case 200:
			final Discovery discovery = request.parseResponse(false);

			if (DEBUG) {
				/* A set containing all possible routes. */
				final Set<String> routes = discovery.routes.keySet();
				
				/* Log all routes for debugging. */
				for (String route : routes) {
					final Route r = discovery.routes.get(route);
					
					LOG.info(route + ":\nGET:" + r.get + ",\tPOST:" + r.post + ",\tPUT:" + r.put + ",\tDELETE:" + r.delete + "\n");
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
	 * Liefert die Veranstaltungen zurück, in die der Nutzer eingetragen ist. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Courses getAllCourses() throws UnauthorizedException, IOException {
		final JacksonRequest<Courses> request = new JacksonRequest<Courses>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses", Courses.class);
		
		switch (request.getCode()) {
		case 200:
			final Courses courses = request.parseResponse(false);
			
			if (DEBUG) {
				for (Course course : courses.courses) {
					LOG.info(course.title + "\n" + course.description + "\n");
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
	 * Liefert alle Semester zurück, in denen der Nutzer in mindestens eine Veranstaltung eingetragen ist.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Courses getAllCoursesBySemesterId(final String semesterId) throws UnauthorizedException, NotFoundException, IOException {
		if (!semesterId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid semester id!");
		}
		
		final JacksonRequest<Courses> request = new JacksonRequest<Courses>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/semester/" + semesterId, Courses.class);

		switch (request.getCode()) {
		case 200:
			final Courses courses = request.parseResponse(false);
			
			if (DEBUG) {
				for (Course course : courses.courses) {
					LOG.info(course.title + "\n" + course.description + "\n");
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
	 * Liefert die Daten der angegebenen Veranstaltung zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Course getCourseById(final String courseId) throws UnauthorizedException, NotFoundException, IOException {
		if (!courseId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid course id!");
		}

		final JacksonRequest<Course> request = new JacksonRequest<Course>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/" + courseId, Course.class);
		
		switch (request.getCode()) {
		case 200:
			final Course course = request.parseResponse(true);
			
			if (DEBUG) {
				LOG.info(course.title + "\n" + course.description + "\n");
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
	 * Liefert die neuen Dateien einer Veranstaltung zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Documents getNewDocumentsByCourseId(final String courseId, final long timestamp) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!courseId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid range id!");
		}
		
		final JacksonRequest<Documents> request = new JacksonRequest<Documents>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + courseId + "/new/" + timestamp, Documents.class);

		switch (request.getCode()) {
		case 200:
			final Documents newDocuments = request.parseResponse(false);
			
			if (DEBUG) {
				LOG.info("Number of new documents: " + newDocuments.documents.size());
				for (Document document : newDocuments.documents) {
					LOG.info(document.name + "\n" + document.description + "\n DocumentId: " + document.document_id + "\n");
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
	 * Liefert die Dateien und Ordner eines angegebenen Ordners einer Veranstaltung zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static DocumentFolders getAllDocumentsByRangeAndFolderId(final String rangeId, final String folderId) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!rangeId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid range id!");
		}
		
		if (folderId != null && !folderId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid folder id!");
		}
		
		final JacksonRequest<DocumentFolders> request = new JacksonRequest<DocumentFolders>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + rangeId + "/folder" + (folderId == null ? "" : "/" + folderId), DocumentFolders.class);

		switch (request.getCode()) {
		case 200:
			final DocumentFolders documentFolders = request.parseResponse(false);
			
			if (DEBUG) {
				LOG.info("Number of subfolders: " + documentFolders.folders.size());
				for (DocumentFolder folder : documentFolders.folders) {
					LOG.info(folder.name + "\n" + folder.description + "\n FolderId: " + folder.folder_id + "\n");
				}
				
				LOG.info("Number of documents: " + documentFolders.documents.size());
				for (Document document : documentFolders.documents) {
					LOG.info(document.name + "\n" + document.description + "\n DocumentId: " + document.document_id + "\n");
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
	 * Liefert die Daten eines Dokuments zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Document getDocumentById(final String documentId) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!documentId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid document id!");
		}

		final JacksonRequest<Document> request = new JacksonRequest<Document>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + documentId, Document.class);
		
		switch (request.getCode()) {
		case 200:
			final Document document = request.parseResponse(false);
			
			if (DEBUG) {
				LOG.info(document.name + "\n");
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
	 * Liefert das Dokument als solches zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static void downloadDocumentById(final String documentId, final File documentFile) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!documentId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid document id!");
		}

		final JacksonRequest<Object> request = new JacksonRequest<Object>(Verb.GET,
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
	 * Liefert alle Semester zurück, in denen der Nutzer in mindestens eine Veranstaltung eingetragen ist.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Semesters getAllSemesters() throws UnauthorizedException, IOException {
		final JacksonRequest<Semesters> request = new JacksonRequest<Semesters>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/semester", Semesters.class);
		
		switch (request.getCode()) {
		case 200:
			final Semesters semesters = request.parseResponse(false);
			
			if (DEBUG) {
				for (Semester semester : semesters.semesters) {
					LOG.info(semester.title + "\n");
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
	 * Liefert die Daten des angegebenen Semesters zurück.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static Semester getSemesterById(final String semesterId) throws UnauthorizedException, NotFoundException, IOException {
		if (!semesterId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid semester id!");
		}

		final JacksonRequest<Semester> request = new JacksonRequest<Semester>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/semesters/" + semesterId, Semester.class);
		
		switch (request.getCode()) {
		case 200:
			final Semester semester = request.parseResponse(false);
			
			if (DEBUG) {
				LOG.info(semester.title + "\n");
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
	 * Liefert die Daten des Nutzers mit der angegebenen Id zurück. Ist keine Id angegeben, so werden die Daten des autorisierten Nutzers zurückgegeben.
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static User getUserById(final String userId) throws UnauthorizedException, NotFoundException, IOException {
		if (userId != null && !userId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid user id!");
		}

		final JacksonRequest<User> request = new JacksonRequest<User>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/user" + (userId == null ? "" : "/" + userId), User.class);

		switch (request.getCode()) {
		case 200:
			final User user = request.parseResponse(true);
			
			if (DEBUG) {
				LOG.info(user.username + "\n");
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
	 * Liefert die Aktivitäten im Umfeld des autorisierten Nutzers zurück.
	 * 
	 * @notice Beta! Funktioniert nicht mit jeder Version.
	 * @return
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 */
	public static Activities getActivities() throws UnauthorizedException, IOException {
		final JacksonRequest<Activities> request = new JacksonRequest<Activities>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/activities", Activities.class);

		switch (request.getCode()) {
		case 200:
			final Activities activities = request.parseResponse(true);
			
			if (DEBUG) {
				for (Activity activity : activities.activities) {
					LOG.info("[" + activity.category + "] " + activity.title + " - " + activity.summary + "\n");
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
