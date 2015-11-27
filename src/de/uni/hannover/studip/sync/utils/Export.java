package de.uni.hannover.studip.sync.utils;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.CourseTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemesterTreeNode;
import de.uni.hannover.studip.sync.datamodel.SemestersTreeNode;
import de.uni.hannover.studip.sync.models.Config;
import de.uni.hannover.studip.sync.models.PathBuilder;
import de.uni.hannover.studip.sync.models.RenameMap;

/**
 * Export helper.
 * 
 * @author Lennart Glauer
 *
 */
public final class Export {
	
	private static final RenameMap RENAMEMAP = RenameMap.getInstance();

	private static final Config CONFIG = Config.getInstance();
	private static final ObjectMapper MAPPER = Config.getMapper();

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
	 * @throws IOException
	 */
	public static void exportMat(final Path rootDirectory, final Path exportDirectory) throws IOException {
		final String folderStructure = CONFIG.getFolderStructure();

		/* Read existing tree. */
		final SemestersTreeNode rootNode = MAPPER.readerFor(SemestersTreeNode.class)
				.readValue(Files.newInputStream(Config.openTreeFile()));

		for (final SemesterTreeNode semester : rootNode.semesters) {
			for (final CourseTreeNode course : semester.courses) {
				if (course.type == 99) {
					// Studiengruppe.
					continue;
				}

				Path courseDirectory = PathBuilder.toPath(folderStructure, rootDirectory, semester, course);
				courseDirectory = rootDirectory.resolve(RENAMEMAP.checkPath(rootDirectory.relativize(courseDirectory).toString()));
				if (!Files.isDirectory(courseDirectory)) {
					continue;
				}

				Path exportCourseDirectory = PathBuilder.toPath(":lecture/:sem/:type", exportDirectory, semester, course);
				exportCourseDirectory = rootDirectory.resolve(RENAMEMAP.checkPath(rootDirectory.relativize(exportCourseDirectory).toString()));
				if (!Files.isDirectory(exportCourseDirectory)) {
					Files.createDirectories(exportCourseDirectory);
				}

				deepCopy(courseDirectory, exportCourseDirectory);
			}
		}
	}
}
