package de.uni.hannover.studip.sync.datamodel;

import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class NewDocumentsModel {

	private SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss");

	private final SimpleObjectProperty<Date> documentChdate;
	private final SimpleStringProperty documentName;
	private final SimpleStringProperty courseTitle;

	public NewDocumentsModel(CourseTreeNode courseNode, DocumentTreeNode documentNode) {
		documentChdate = new SimpleObjectProperty<Date>(new Date(documentNode.chdate * 1000L) {
			private static final long serialVersionUID = 1L;

			@Override
			public String toString() {
				return dateFormat.format(this);
			}
		});

		documentName = new SimpleStringProperty(documentNode.name);

		courseTitle = new SimpleStringProperty(courseNode.title);
	}

	public Date getDocumentChdate() {
		return documentChdate.get();
	}

	public String getDocumentName() {
		return documentName.get();
	}

	public String getCourseTitle() {
		return courseTitle.get();
	}
}