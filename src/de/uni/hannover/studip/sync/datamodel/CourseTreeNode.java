package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.elanev.studip.android.app.backend.datamodel.Course;

/**
 * Course tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseTreeNode {

	public String course_id;
	public String title;
	public Long start_time;
	public Long duration_time;
	/* "1": Vorlesung, "2": Seminar, "3": Übung, "4": Projekt, "99": Studiengruppe. */
	public int type;

	/* Last update time used for request caching. */
	public long update_time;

	/* Child nodes. */
	public DocumentFolderTreeNode root = new DocumentFolderTreeNode();

	public CourseTreeNode() {
		// Needed for json object binding.
	}

	public CourseTreeNode(final Course course) {
		this.course_id = course.courseId;
		this.title = course.title;
		this.start_time = course.startTime;
		this.duration_time = course.durationTime;
		this.type = course.type;
		this.update_time = System.currentTimeMillis() / 1000L;
	}

}
