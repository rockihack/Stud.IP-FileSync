package de.uni.hannover.studip.sync.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.elanev.studip.android.app.backend.datamodel.Document;

/**
 * Document tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentTreeNode {

	public String documentId;
	public String userId;
	public String name;
	public Long mkDate;
	public Long chDate;
	public String fileName;
	public Long fileSize;

	public DocumentTreeNode() {
		// Needed for json object binding.
	}

	public DocumentTreeNode(final Document document) {
		this.documentId = document.document_id;
		this.userId = document.user_id;
		this.name = document.name;
		this.mkDate = document.mkdate;
		this.chDate = document.chdate;
		this.fileName = document.filename;
		this.fileSize = document.filesize;
	}
	
}
