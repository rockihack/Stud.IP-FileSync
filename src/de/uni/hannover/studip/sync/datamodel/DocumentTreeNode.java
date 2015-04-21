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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		
		result = prime * result + ((chdate == null) ? 0 : chdate.hashCode());
		result = prime * result
				+ ((document_id == null) ? 0 : document_id.hashCode());
		
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
		
		DocumentTreeNode other = (DocumentTreeNode) obj;
		
		if (chdate == null) {
			if (other.chdate != null)
				return false;
		} else if (!chdate.equals(other.chdate))
			return false;
		
		if (document_id == null) {
			if (other.document_id != null)
				return false;
		} else if (!document_id.equals(other.document_id))
			return false;
		
		return true;
	}

	public String document_id;
	public String user_id;
	public String name;
	public Long mkdate;
	public Long chdate;
	public String filename;
	public Long filesize;
	public String mime_type;

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
		this.mime_type = document.mime_type;
	}
	
}
