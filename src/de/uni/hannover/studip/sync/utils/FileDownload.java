package de.uni.hannover.studip.sync.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
//import java.nio.channels.Channels;
//import java.nio.channels.ReadableByteChannel;

/**
 * File download helper.
 * 
 * @author Lennart Glauer
 * @see http://stackoverflow.com/questions/921262/how-to-download-and-save-a-file-from-internet-using-java
 */
public final class FileDownload {

	/*@SuppressWarnings("resource")
	public static void get(InputStream is, String path) throws IOException {
		ReadableByteChannel rbc = Channels.newChannel(is);
		FileOutputStream fos = new FileOutputStream(path);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
	}*/

	private FileDownload() {
		// Utility class.
	}

	private static final int BUFFER_SIZE = 8192;

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
