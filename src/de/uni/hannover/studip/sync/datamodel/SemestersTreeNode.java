package de.uni.hannover.studip.sync.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Semesters tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SemestersTreeNode {
	
	public List<SemesterTreeNode> semesters = Collections.synchronizedList(new ArrayList<SemesterTreeNode>());
	
}
