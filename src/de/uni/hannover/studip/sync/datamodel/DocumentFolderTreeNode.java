package de.uni.hannover.studip.sync.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.elanev.studip.android.app.backend.datamodel.DocumentFolder;

/**
 * Folder tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentFolderTreeNode {
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		result = prime * result + ((chdate == null) ? 0 : chdate.hashCode());
		result = prime * result
				+ ((folder_id == null) ? 0 : folder_id.hashCode());
		
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		
		DocumentFolderTreeNode other = (DocumentFolderTreeNode) obj;
		
		if (chdate == null) {
			if (other.chdate != null)
				return false;
		} else if (!chdate.equals(other.chdate))
			return false;
		
		if (folder_id == null) {
			if (other.folder_id != null)
				return false;
		} else if (!folder_id.equals(other.folder_id))
			return false;
		
		return true;
	}

	public String folder_id;
	public String user_id;
	public String name;
	public String mkdate;
	public Long chdate;
	
	/* Child nodes. */
	public List<DocumentFolderTreeNode> folders = Collections.synchronizedList(new ArrayList<DocumentFolderTreeNode>());
	public List<DocumentTreeNode> documents = Collections.synchronizedList(new ArrayList<DocumentTreeNode>());
	
	public DocumentFolderTreeNode() {
	}
	
	public DocumentFolderTreeNode(DocumentFolder folder) {
		this.folder_id = folder.folder_id;
		this.user_id = folder.user_id;
		this.name = folder.name;
		this.mkdate = folder.mkdate;
		this.chdate = folder.chdate;
	}
	
}
