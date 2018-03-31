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
	public Name name;

	/**
	 * Default constructor
	 */
	public User() {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public class Name {
		public String family;
		public String given;
		public String prefix;
		public String suffix;
		public String formatted;

		public Name() {
		}

	}

}
