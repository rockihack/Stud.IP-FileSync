package de.uni.hannover.studip.sync.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.elanev.studip.android.app.backend.datamodel.Semester;

/**
 * Semester tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemesterTreeNode {

	public String semester_id;
	public String title;
	public long begin;
	public long end;
	public long seminars_begin;
	public long seminars_end;

	/* Child nodes. */
	public final List<CourseTreeNode> courses = Collections.synchronizedList(new ArrayList<CourseTreeNode>());

	public SemesterTreeNode() {
		// Needed for json object binding.
	}

	public SemesterTreeNode(final Semester semester) {
		this.semester_id = semester.semester_id;
		this.title = semester.title;
		this.begin = semester.begin;
		this.end = semester.end;
		this.seminars_begin = semester.seminars_begin;
		this.seminars_end = semester.seminars_end;
	}

}
