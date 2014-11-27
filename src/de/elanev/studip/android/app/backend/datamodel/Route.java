package de.elanev.studip.android.app.backend.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Route data model.
 * 
 * @author Lennart Glauer
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Route {

	public boolean get;
	public boolean post;
	public boolean put;
	public boolean delete;
	
}
