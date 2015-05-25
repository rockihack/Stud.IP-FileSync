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

	public String semesterId;
	public String title;
	public long begin;
	public long end;
	public long seminarsBegin;
	public long seminarsEnd;

	/* Child nodes. */
	public final List<CourseTreeNode> courses = Collections.synchronizedList(new ArrayList<CourseTreeNode>());

	public SemesterTreeNode() {
		// Needed for json object binding.
	}

	public SemesterTreeNode(final Semester semester) {
		this.semesterId = semester.semester_id;
		this.title = semester.title;
		this.begin = semester.begin;
		this.end = semester.end;
		this.seminarsBegin = semester.seminars_begin;
		this.seminarsEnd = semester.seminars_end;
	}

}
