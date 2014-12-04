package de.elanev.studip.android.app.backend.datamodel;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activities {

	public ArrayList<Activity> activities;

	public Activities() {
		this.activities = new ArrayList<Activity>();
	}
	
}
