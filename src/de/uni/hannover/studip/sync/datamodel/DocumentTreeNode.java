package de.uni.hannover.studip.sync.datamodel;

import de.elanev.studip.android.app.backend.datamodel.Document;

/**
 * Document tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class DocumentTreeNode {

	public String documentId;
	public String userId;
	public String name;
	public String description;
	public Long mkDate;
	public Long chDate;
	public Long fileSize;

	public DocumentTreeNode() {
		// Needed for json object binding.
	}

	public DocumentTreeNode(final Document document) {
		this.documentId = document.file_id;
		this.userId = document.user_id;
		this.name = document.name;
		this.description = document.description;
		this.mkDate = document.mkdate;
		this.chDate = document.chdate;
		this.fileSize = document.size;
	}
	
}
