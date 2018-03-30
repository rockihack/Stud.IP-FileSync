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

import java.util.HashMap;
import java.util.Map;

/**
 * POJO class that stores a list of Course objects.
 *
 * @author joern
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Courses {
  public Map<String, Course> collection;

  /**
   * Default constructor that creates an empty courses ArrayList
   */
  public Courses() {
	  collection = new HashMap<String, Course>();
  }

}
