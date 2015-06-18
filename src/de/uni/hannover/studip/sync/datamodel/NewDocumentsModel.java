package de.uni.hannover.studip.sync.datamodel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class NewDocumentsModel {

	protected final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.GERMANY);

	private final SimpleStringProperty semesterTitle;
	private final SimpleStringProperty courseTitle;
	private final SimpleObjectProperty<Date> documentChDate;
	private final SimpleStringProperty documentName;
	private final String documentDescription;
	private final File documentFile;

	/**
	 * 
	 * @param courseNode
	 * @param documentNode
	 * @param file
	 */
	public NewDocumentsModel(final SemesterTreeNode semesterNode, final CourseTreeNode courseNode, final DocumentTreeNode documentNode, final File file) {
		semesterTitle = new SimpleStringProperty(semesterNode.title);
		courseTitle = new SimpleStringProperty(courseNode.title);

		documentChDate = new SimpleObjectProperty<Date>(new Date(documentNode.chDate * 1000L) {
			private static final long serialVersionUID = 1L;

			@Override
			public String toString() {
				return dateFormat.format(this);
			}
		});

		documentName = new SimpleStringProperty(documentNode.name);
		documentDescription = documentNode.description.isEmpty() ? "Keine Beschreibung vorhanden" : documentNode.description;
		documentFile = file;
	}

	/**
	 * 
	 * @return
	 */
	public String getSemesterTitle() {
		return semesterTitle.get();
	}

	/**
	 * 
	 * @return
	 */
	public String getCourseTitle() {
		return courseTitle.get();
	}

	/**
	 * 
	 * @return
	 */
	public Date getDocumentChdate() {
		return documentChDate.get();
	}

	/**
	 * 
	 * @return
	 */
	public String getDocumentName() {
		return documentName.get();
	}

	/**
	 * 
	 * @return
	 */
	public String getDocumentDescription() {
		return documentDescription;
	}

	/**
	 * 
	 * @return
	 */
	public File getDocumentFile() {
		return documentFile;
	}
}