/*******************************************************************************
 * Copyright (c) 2013 ELAN e.V.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
/**
 * 
 */
package de.elanev.studip.android.app.backend.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author joern
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
	public String user_id;
	public String username;

	/**
	 * Default constructor
	 */
	public User() {
	}

	/**
	 * @param user_id
	 * @param username
	 * @param perms
	 * @param title_pre
	 * @param forename
	 * @param lastname
	 * @param title_post
	 * @param email
	 * @param avatar_small
	 * @param avatar_medium
	 * @param avatar_normal
	 * @param phone
	 * @param homepage
	 * @param privadr
	 * @param role
	 */
	public User(String user_id, String username) {
		this.user_id = user_id;
		this.username = username;
	}

	public String getFullName() {
		return this.username; // TODO
	}

	public String getName() {
		return this.username; // TODO
	}

}
