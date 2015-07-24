package de.uni.hannover.studip.sync.utils;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.PathBuilder;

/**
 * Export helper.
 * 
 * @author Lennart Glauer
 *
 */
public final class Export {

	private Export() {
		// Utility class.
	}

	/**
	 * Create a deep copy of a directory.
	 * 
	 * @param source
	 * @param destination
	 * @throws IOException
	 */
	private static void deepCopy(final Path source, final Path destination) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
				final Path destDir = destination.resolve(source.relativize(dir));

				if (!Files.isDirectory(destDir)) {
					Files.createDirectory(destDir);
				}

				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
				final Path destFile = destination.resolve(source.relativize(file));

				Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Export Materialsammlung.
	 * 
	 * @param rootDirectory
	 * @param exportDirectory
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public static void exportMat(final Path rootDirectory, final Path exportDirectory) throws JsonParseException, JsonMappingException, IOException {
		/* Read existing tree. */
		final ObjectMapper mapper = new ObjectMapper();
		final SemestersTreeNode rootNode = mapper.readValue(Files.newBufferedReader(Config.openTreeFile()), SemestersTreeNode.class);

		for (SemesterTreeNode semester : rootNode.semesters) {
			for (CourseTreeNode course : semester.courses) {
				if (course.type == 99) {
					// Studiengruppe.
					continue;
				}

				final String folderStructure = Config.getInstance().getFolderStructure();
				final Path courseDirectory = new PathBuilder(folderStructure, rootDirectory, semester, course).toPath();
				if (!Files.isDirectory(courseDirectory)) {
					continue;
				}

				final Path exportCourseDirectory = new PathBuilder(":lecture/:sem/:type", exportDirectory, semester, course).toPath();
				if (!Files.isDirectory(exportCourseDirectory)) {
					Files.createDirectories(exportCourseDirectory);
				}

				Export.deepCopy(courseDirectory, exportCourseDirectory);
			}
		}
	}
}
