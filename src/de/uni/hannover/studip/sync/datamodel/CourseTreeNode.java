package de.uni.hannover.studip.sync.datamodel;

import de.elanev.studip.android.app.backend.datamodel.Course;

/**
 * Course tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class CourseTreeNode {

	public String courseId;
	public String title;
	public Long startTime;
	public Long durationTime;
	/*
	 * Stud.IP default values:
	 * 1: Vorlesung
	 * 2: Seminar
	 * 3: Übung
	 * 4: Praktikum
	 * 5: Colloquium
	 * 6: Forschungsgruppe
	 * 7: sonstige
	 * 8: Gremium
	 * 9: Projektgruppe
	 * 10: sonstige
	 * 11: Kulturforum
	 * 12: Veranstaltungsboard
	 * 13: sonstige
	 * 99: Studiengruppe
	 */
	public int type;

	/* Last update time used for request caching. */
	public long updateTime;

	/* Child nodes. */
	public DocumentFolderTreeNode root = new DocumentFolderTreeNode();

	public CourseTreeNode() {
		// Needed for json object binding.
	}

	public CourseTreeNode(final Course course) {
		this.courseId = course.courseId;
		this.title = course.title;
		this.startTime = course.startTime;
		this.durationTime = course.durationTime;
		this.type = course.type;
		this.updateTime = System.currentTimeMillis() / 1000L;
	}

}
