package de.uni.hannover.studip.sync.datamodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.elanev.studip.android.app.backend.datamodel.DocumentFolder;

/**
 * Folder tree node used for json object binding.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public class DocumentFolderTreeNode {

	public String folderId;
	public String userId;
	public String name;
	public String mkDate;
	public Long chDate;

	/* Child nodes. */
	public final List<DocumentFolderTreeNode> folders = Collections.synchronizedList(new ArrayList<DocumentFolderTreeNode>());
	public final List<DocumentTreeNode> documents = Collections.synchronizedList(new ArrayList<DocumentTreeNode>());

	public DocumentFolderTreeNode() {
		// Course root node.
	}

	public DocumentFolderTreeNode(final DocumentFolder folder) {
		this.folderId = folder.folder_id;
		this.userId = folder.user_id;
		this.name = folder.name;
		this.mkDate = folder.mkdate;
		this.chDate = folder.chdate;
	}

}
