package de.uni.hannover.studip.sync.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Semesters tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class SemestersTreeNode {

	public final List<SemesterTreeNode> semesters = Collections.synchronizedList(new ArrayList<SemesterTreeNode>());

}
