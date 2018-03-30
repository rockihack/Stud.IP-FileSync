/*******************************************************************************
 * Copyright (c) 2013 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package de.elanev.studip.android.app.backend.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author joern
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Document {
	public String file_id;
	public String folder_id;
	public String user_id;
	public String name;
	public String description;
	public Long mkdate;
	public Long chdate;
	public Long size;
	public Integer downloads;
	public String mime_type;

	public Document() {
	}

	/**
	 * @param document_id
	 * @param user_id
	 * @param name
	 * @param description
	 * @param mkdate
	 * @param chdate
	 * @param filename
	 * @param filesize
	 * @param downloads
	 * @param file_protected
	 * @param mime_type
	 */
	public Document(String file_id, String folder_id, String user_id,
			String name, String description, Long mkdate, Long chdate,
			String filename, Long filesize, Integer downloads,
			Boolean file_protected, String mime_type) {
		this.file_id = file_id;
		this.folder_id = folder_id;
		this.user_id = user_id;
		this.name = name;
		this.description = description;
		this.mkdate = mkdate;
		this.chdate = chdate;
		this.size = filesize;
		this.downloads = downloads;
		this.mime_type = mime_type;
	}
}
