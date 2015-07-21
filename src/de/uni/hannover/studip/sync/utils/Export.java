package de.uni.hannover.studip.sync.utils;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.models.Config;

/**
 * Export helper.
 * 
 * @author Lennart Glauer
 *
 */
public final class Export {

	private static final Logger LOG = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	private Export() {
		// Utility class.
	}

	/**
	 * Create a deep copy of a directory.
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	private static void deepCopy(final Path source, final Path destination) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
				final Path destDir = destination.resolve(source.relativize(dir));

				if (!Files.isDirectory(destDir)) {
					Files.createDirectory(destDir);
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				final Path destFile = destination.resolve(source.relativize(file));

				Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Get course title.
	 * 
	 * @param courses
	 * @param course
	 * @return
	 */
	private static String getCourseMatTitle(final List<CourseTreeNode> courses, final CourseTreeNode course) {
		final String courseTitle = FileBrowser.removeIllegalCharacters(course.title);
		final String courseTitleLowerCase = courseTitle.toLowerCase(Locale.GERMANY);

		if (course.type == 3 || courseTitleLowerCase.contains("übung") || courseTitleLowerCase.contains("uebung")) {
			// Search lecture course for this exercise.
			for (CourseTreeNode lecture : courses) {
				final String lectureTitle = FileBrowser.removeIllegalCharacters(lecture.title);
				final String lectureTitleLowerCase = lectureTitle.toLowerCase(Locale.GERMANY);

				if (!course.courseId.equals(lecture.courseId)
						&& courseTitleLowerCase.contains(lectureTitleLowerCase)) {
					return lectureTitle;
				}
			}

			if (LOG.isLoggable(Level.WARNING)) {
				LOG.warning("No lecture course found for " + courseTitle);
			}
		}

		return courseTitle;
	}

	/**
	 * Return the short name of given semester (e.g. 15ss or 15ws).
	 * 
	 * @param begin
	 * @return
	 */
	private static String getSemesterShortTitle(final long begin) {
		final Calendar calendar = Calendar.getInstance(Locale.GERMANY);
		calendar.setTimeInMillis(begin * 1000L);

		final StringBuilder str = new StringBuilder();
		str.append(calendar.get(Calendar.YEAR) % 100);
		str.append(calendar.get(Calendar.MONTH) < 6 ? "ss" : "ws");

		return str.toString();
	}

	/**
	 * Export Materialsammlung.
	 * 
	 * @param rootDirectory
	 * @param exportDirectory
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static void exportMat(final Path rootDirectory, final Path exportDirectory) throws JsonParseException, JsonMappingException, IOException {
		/* Read existing tree. */
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(Files.newBufferedReader(Config.openTreeFile()), SemestersTreeNode.class);

		for (SemesterTreeNode semester : rootNode.semesters) {
			final String semesterShortTitle = getSemesterShortTitle(semester.begin);
			final String semesterTitle = FileBrowser.removeIllegalCharacters(semester.title);
			final Path semesterDirectory = rootDirectory.resolve(semesterTitle);

			for (CourseTreeNode course : semester.courses) {
				if (course.type == 99) {
					// Studiengruppe.
					continue;
				}

				final String courseTitle = FileBrowser.removeIllegalCharacters(course.title);
				final Path courseDirectory = semesterDirectory.resolve(courseTitle);

				final Path courseExport = exportDirectory.resolve(getCourseMatTitle(semester.courses, course));
				if (!Files.isDirectory(courseExport)) {
					Files.createDirectory(courseExport);
				}

				final Path semesterExport = courseExport.resolve(semesterShortTitle);
				if (!Files.isDirectory(semesterExport)) {
					Files.createDirectory(semesterExport);
				}

				Path typeExport;

				switch (course.type) {
				case 1: // Vorlesung.
					typeExport = semesterExport.resolve("vorlesung");
					break;
				case 2: // Seminar.
					typeExport = semesterExport.resolve("seminar");
					break;
				case 3: // Übung.
					typeExport = semesterExport.resolve("uebung");
					break;
				case 4: // Praktikum.
					typeExport = semesterExport.resolve("praktikum");
					break;
				default:
					// Try to recover type from title.
					final String courseTitleLowerCase = courseTitle.toLowerCase(Locale.GERMANY);
					if (courseTitleLowerCase.contains("vorlesung")) {
						typeExport = semesterExport.resolve("vorlesung");
					} else if (courseTitleLowerCase.contains("seminar")) {
						typeExport = semesterExport.resolve("seminar");
					} else if (courseTitleLowerCase.contains("übung") || courseTitleLowerCase.contains("uebung")) {
						typeExport = semesterExport.resolve("uebung");
					} else if (courseTitleLowerCase.contains("praktikum")) {
						typeExport = semesterExport.resolve("praktikum");
					} else if (courseTitleLowerCase.contains("projekt")) {
						typeExport = semesterExport.resolve("projekt");
					} else {
						typeExport = semesterExport.resolve("default");
					}
					break;
				}

				Export.deepCopy(courseDirectory, typeExport);
			}
		}
	}
}
