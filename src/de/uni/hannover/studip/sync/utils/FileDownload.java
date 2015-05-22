package de.uni.hannover.studip.sync.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File download utility class.
 * 
 * @author Lennart Glauer
 * @see http://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
 */
public final class FileDownload {

	private static final int BUFFER_SIZE = 8192;

	private FileDownload() {
		// Utility class.
	}

	/**
	 * Write inputstream to file.
	 * 
	 * @param is
	 * @param file
	 * @throws IOException
	 */
	public static void get(final InputStream is, final File file) throws IOException {
		try (BufferedInputStream in = new BufferedInputStream(is); FileOutputStream out = new FileOutputStream(file)) {
			final byte buffer[] = new byte[BUFFER_SIZE];
			int count;

			while ((count = in.read(buffer, 0, BUFFER_SIZE)) > 0) {
				out.write(buffer, 0, count);
			}
		}
	}
	
}
