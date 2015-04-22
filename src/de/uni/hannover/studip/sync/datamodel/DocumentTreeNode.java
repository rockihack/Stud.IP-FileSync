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

	public String document_id;
	public String user_id;
	public String name;
	public Long mkdate;
	public Long chdate;
	public String filename;
	public Long filesize;

	public DocumentTreeNode() {
	}
	
	public DocumentTreeNode(Document document) {
		this.document_id = document.document_id;
		this.user_id = document.user_id;
		this.name = document.name;
		this.mkdate = document.mkdate;
		this.chdate = document.chdate;
		this.filename = document.filename;
		this.filesize = document.filesize;
	}
	
}
