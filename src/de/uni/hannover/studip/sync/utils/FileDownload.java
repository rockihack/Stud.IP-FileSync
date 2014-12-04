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
public class FileDownload {

	/*@SuppressWarnings("resource")
	public static void get(InputStream is, String path) throws IOException {
		ReadableByteChannel rbc = Channels.newChannel(is);
		FileOutputStream fos = new FileOutputStream(path);
		fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
	}*/
	
	private static final int BUFFER_SIZE = 8192;
	
	public static void get(InputStream is, File file) throws IOException {
		BufferedInputStream in = null;
		FileOutputStream out = null;
		
		try{
			in = new BufferedInputStream(is);
			out = new FileOutputStream(file);
			
			byte buffer[] = new byte[BUFFER_SIZE];
			int count;
			
			while ((count = in.read(buffer, 0, BUFFER_SIZE)) > 0) {
				out.write(buffer, 0, count);
				System.out.println(count);
			}
			
		} finally {
			if (in != null) {
			    in.close();
			}
			if (out != null) {
			    out.close();
			}
		}
	}
	
}
