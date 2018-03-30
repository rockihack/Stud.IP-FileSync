package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Path;

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
	 * Regex for studip id (MD5) validation.
	 */
	private static final String STUDIP_ID_REGEX = "^[a-f0-9]{32}$";

	private RestApi() {
		// Utility class.
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

		final JacksonRequest<Courses> request = new JacksonRequest<>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/semester/" + semesterId, Courses.class);

		switch (request.getCode()) {
		case 200:
			return request.parseResponse();
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

		final JacksonRequest<Documents> request = new JacksonRequest<>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + courseId + "/new/" + timestamp, Documents.class);

		switch (request.getCode()) {
		case 200:
			return request.parseResponse();
		case 400:
			throw new NotFoundException("Not found!");
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 403:
			throw new ForbiddenException("Forbidden!");
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

		final JacksonRequest<DocumentFolders> request = new JacksonRequest<>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + rangeId + "/folder" + (folderId == null ? "" : "/" + folderId), DocumentFolders.class);

		switch (request.getCode()) {
		case 200:
			return request.parseResponse();
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
	 * Liefert das Dokument als solches zurück. 
	 * 
	 * @return
	 * @throws UnauthorizedException 
	 * @throws ForbiddenException 
	 * @throws NotFoundException 
	 * @throws IOException 
	 */
	public static long downloadDocumentById(final String documentId, final Path documentFile) throws UnauthorizedException, ForbiddenException, NotFoundException, IOException {
		if (!documentId.matches(STUDIP_ID_REGEX)) {
			throw new IllegalArgumentException("Invalid document id!");
		}

		final JacksonRequest<Object> request = new JacksonRequest<>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/documents/" + documentId + "/download", Object.class);

		switch (request.getCode()) {
		case 200:
			return FileDownload.get(request.getStream(), documentFile);
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
		final JacksonRequest<Semesters> request = new JacksonRequest<>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/courses/semester", Semesters.class);

		switch (request.getCode()) {
		case 200:
			return request.parseResponse();
		case 401:
			throw new UnauthorizedException("Unauthorized!");
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

		final JacksonRequest<User> request = new JacksonRequest<>(Verb.GET,
				StudIPApiProvider.BASE_URL + "/api/user" + (userId == null ? "" : "/" + userId), User.class);

		switch (request.getCode()) {
		case 200:
			return request.parseResponse();
		case 401:
			throw new UnauthorizedException("Unauthorized!");
		case 404:
			throw new NotFoundException("Not found!");
		default:
			throw new IllegalStateException("Statuscode: " + request.getCode());
		}
	}
}
