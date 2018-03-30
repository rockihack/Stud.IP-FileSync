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
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * POJO that represents the response of the /courses/:course_id route.
 *
 * @author joern
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Course {
  @JsonProperty("course_id")
  public String courseId;
  @JsonProperty("title")
  public String title;
  @JsonProperty("subtitle")
  public String subtitle;
  @JsonProperty("description")
  public String description;
  @JsonProperty("type")
  public int type;

  public Course() {
  }

}
