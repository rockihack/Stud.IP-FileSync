package de.uni.hannover.studip.sync.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHash {
	
	private static final int BUFFER_SIZE = 8192;
	
	private static final char[] HEX = "0123456789abcdef".toCharArray();

	public static String getMd5(File file) throws FileNotFoundException, IOException, NoSuchAlgorithmException {
		try (FileInputStream in = new FileInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance("MD5");
			
			byte buffer[] = new byte[BUFFER_SIZE];
			int count;
			
			while ((count = in.read(buffer, 0, BUFFER_SIZE)) > 0) {
				digest.update(buffer, 0, count);
			}
			
			return bytesToHex(digest.digest());
		}
	}
	
	private static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		
		for ( int j = 0; j < bytes.length; j++ ) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = HEX[v >>> 4];
			hexChars[j * 2 + 1] = HEX[v & 0x0F];
		}
		
		return new String(hexChars);
	}
}
