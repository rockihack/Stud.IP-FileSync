package de.uni.hannover.studip.sync.models;

import java.nio.file.Path;
import java.util.Calendar;
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
public class PathBuilder {

	/**
	 * Path template (e.g. ":semester/:course" or ":semester/:lecture/:type").
	 */
	private final String template;

	/**
	 * Root directory.
	 */
	private final Path rootDir;

	/**
	 * Semester tree node.
	 */
	private final SemesterTreeNode semester;

	/**
	 * Course tree node.
	 */
	private final CourseTreeNode course;

	public PathBuilder(final String template, final Path rootDir, final SemesterTreeNode semester, final CourseTreeNode course) {
		this.template = template;
		this.rootDir = rootDir;
		this.semester = semester;
		this.course = course;
	}

	/**
	 * Return the short name of given semester (e.g. 15ss or 15ws).
	 * 
	 * @return
	 */
	private String getSemesterShortTitle() {
		final Calendar calendar = Calendar.getInstance(Locale.GERMANY);
		calendar.setTimeInMillis(semester.begin * 1000L);

		final StringBuilder str = new StringBuilder();
		str.append(calendar.get(Calendar.YEAR) % 100)
			.append(calendar.get(Calendar.MONTH) < 6 ? "ss" : "ws");

		return str.toString();
	}

	/**
	 * Match exercises with lectures and return course title.
	 * 
	 * @return
	 */
	private String getCourseTitle() {
		final String courseTitle = FileBrowser.removeIllegalCharacters(course.title);
		final String courseTitleLowerCase = courseTitle.toLowerCase(Locale.GERMANY);

		if (course.type == 3 || courseTitleLowerCase.contains("übung") || courseTitleLowerCase.contains("uebung")) {
			// Search lecture for this exercise.
			for (CourseTreeNode lecture : semester.courses) {
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
	private String getCourseType() {
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
	}

	/**
	 * String.
	 */
	@Override
	public String toString() {
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
				str.append(getSemesterShortTitle());
				break;
			case ":lecture":
				str.append(getCourseTitle());
				break;
			case ":type":
				str.append(getCourseType());
				break;
			default:
				throw new IllegalArgumentException("Invalid folder structure!");
			}
			str.append('/');
		}

		return str.toString();
	}

	/**
	 * Path.
	 * 
	 * @return
	 */
	public Path toPath() {
		return rootDir.resolve(toString());
	}
}
