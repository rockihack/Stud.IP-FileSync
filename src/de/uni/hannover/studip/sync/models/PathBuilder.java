package de.uni.hannover.studip.sync.models;

import java.nio.file.Path;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.StringTokenizer;

import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.utils.FileBrowser;

/**
 * 
 * @author Lennart Glauer
 *
 */
public final class PathBuilder {

	private PathBuilder() {
		// Utility class.
	}

	/**
	 * Return the short name of given semester (e.g. 15ss or 15ws).
	 * 
	 * @return
	 */
	private static String getSemesterShortTitle(final SemesterTreeNode semester) {
		final GregorianCalendar calendar = new GregorianCalendar(Locale.GERMANY);
		calendar.setTimeInMillis(semester.begin * 1000L);

		final StringBuilder str = new StringBuilder();
		str.append(calendar.get(GregorianCalendar.YEAR) % 100)
			.append(calendar.get(GregorianCalendar.MONTH) < 6 ? "ss" : "ws");

		return str.toString();
	}

	/**
	 * Match exercises with lectures and return course title.
	 * 
	 * @return
	 */
	private static String getCourseTitle(final SemesterTreeNode semester, final CourseTreeNode course) {
		final String courseTitle = FileBrowser.removeIllegalCharacters(course.title);
		final String courseTitleLowerCase = courseTitle.toLowerCase(Locale.GERMANY);

		if (course.type == 3 || courseTitleLowerCase.contains("übung") || courseTitleLowerCase.contains("uebung")) {
			// Search lecture for this exercise.
			for (final CourseTreeNode lecture : semester.courses) {
				final String lectureTitle = FileBrowser.removeIllegalCharacters(lecture.title);
				final String lectureTitleLowerCase = lectureTitle.toLowerCase(Locale.GERMANY);

				if (!course.courseId.equals(lecture.courseId)
						&& courseTitleLowerCase.contains(lectureTitleLowerCase)) {
					return lectureTitle;
				}
			}
		}

		return courseTitle;
	}

	/**
	 * Get course type.
	 * 
	 * @return
	 */
	private static String getCourseType(final CourseTreeNode course) {
		switch (course.type) {
		case 1:
			return "vorlesung";
		case 2:
			return "seminar";
		case 3:
			return "uebung";
		case 4:
			return "praktikum";
		case 99:
			return "studiengruppe";
		default:
			return getCourseTypeByTitle(course);
		}
	}

	private static String getCourseTypeByTitle(final CourseTreeNode course) {
		final String courseTitleLowerCase = course.title.toLowerCase(Locale.GERMANY);
		if (courseTitleLowerCase.contains("seminar")) {
			return "seminar";
		} else if (courseTitleLowerCase.contains("übung") || courseTitleLowerCase.contains("uebung")) {
			return "uebung";
		} else if (courseTitleLowerCase.contains("praktikum")) {
			return "praktikum";
		} else if (courseTitleLowerCase.contains("projekt")) {
			return "projekt";
		} else {
			return "vorlesung";
		}
	}

	public static String toString(final String template, final SemesterTreeNode semester, final CourseTreeNode course) {
		final StringBuilder str = new StringBuilder();
		final StringTokenizer tokens = new StringTokenizer(template, "/");

		while(tokens.hasMoreTokens()) {
			switch(tokens.nextToken()) {
			case ":semester":
				str.append(FileBrowser.removeIllegalCharacters(semester.title));
				break;
			case ":course":
				str.append(FileBrowser.removeIllegalCharacters(course.title));
				break;
			case ":sem":
				str.append(getSemesterShortTitle(semester));
				break;
			case ":lecture":
				str.append(getCourseTitle(semester, course));
				break;
			case ":type":
				str.append(getCourseType(course));
				break;
			default:
				throw new IllegalArgumentException("Invalid folder structure!");
			}
			str.append('/');
		}

		return str.toString();
	}

	public static Path toPath(final String template, final Path rootDir, final SemesterTreeNode semester, final CourseTreeNode course) {
		return rootDir.resolve(toString(template, semester, course));
	}
}
