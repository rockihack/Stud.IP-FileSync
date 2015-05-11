package de.uni.hannover.studip.sync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * File hash utility class.
 * 
 * @author Lennart Glauer
 *
 */
public final class FileHash {

	private static final int BUFFER_SIZE = 8192;

	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private FileHash() {
		// Utility class.
	}

	/**
	 * Generate md5 hash and return it as lowercase string.
	 * 
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
	public static String getMd5(final File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		try (FileInputStream in = new FileInputStream(file)) {
			final MessageDigest digest = MessageDigest.getInstance("MD5");

			final byte buffer[] = new byte[BUFFER_SIZE];
			int count;

			while ((count = in.read(buffer, 0, BUFFER_SIZE)) > 0) {
				digest.update(buffer, 0, count);
			}

			return bytesToHex(digest.digest());
		}
	}

	/**
	 * Convert byte (hex) array to human readable string.
	 * 
	 * @param bytes
	 * @return
	 */
	private static String bytesToHex(final byte[] bytes) {
		final char[] hexChars = new char[bytes.length * 2];

		for ( int j = 0; j < bytes.length; j++ ) {
			final int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX[v >>> 4];
			hexChars[j * 2 + 1] = HEX[v & 0x0F];
		}

		return new String(hexChars);
	}
}
