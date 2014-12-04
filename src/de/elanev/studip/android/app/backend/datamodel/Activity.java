package de.elanev.studip.android.app.backend.datamodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Activity {

	public String activity_id;
	public String title;
	public String author;
	public String author_id;
	public String link;
	public int updated;
	public String summary;
	public String content;
	public String category;
	
	public Activity(String activity_id, String title, String author,
			String author_id, String link, int updated, String summary,
			String content, String category) {

		this.activity_id = activity_id;
		this.title = title;
		this.author = author;
		this.author_id = author_id;
		this.link = link;
		this.updated = updated;
		this.summary = summary;
		this.content = content;
		this.category = category;
	}
	
}
