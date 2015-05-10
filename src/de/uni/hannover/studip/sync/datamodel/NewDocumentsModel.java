package de.uni.hannover.studip.sync.datamodel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

public class NewDocumentsModel {

	protected final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.GERMANY);

	private final SimpleObjectProperty<Date> documentChdate;
	private final SimpleStringProperty documentName;
	private final SimpleStringProperty courseTitle;
	private final File documentFile;

	/**
	 * 
	 * @param courseNode
	 * @param documentNode
	 * @param file
	 */
	public NewDocumentsModel(final CourseTreeNode courseNode, final DocumentTreeNode documentNode, final File file) {
		documentChdate = new SimpleObjectProperty<Date>(new Date(documentNode.chdate * 1000L) {
			private static final long serialVersionUID = 1L;

			@Override
			public String toString() {
				return dateFormat.format(this);
			}
		});

		documentName = new SimpleStringProperty(documentNode.name);
		courseTitle = new SimpleStringProperty(courseNode.title);
		documentFile = file;
	}

	/**
	 * 
	 * @return
	 */
	public Date getDocumentChdate() {
		return documentChdate.get();
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
	public String getCourseTitle() {
		return courseTitle.get();
	}

	/**
	 * 
	 * @return
	 */
	public File getDocumentFile() {
		return documentFile;
	}
}