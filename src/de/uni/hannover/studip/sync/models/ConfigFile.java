package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 * @author Lennart Glauer
 *
 * @param <T>
 */
public class ConfigFile<T> {

	private final File file;
	private final Class<T> type;

	public T data;

	/**
	 * 
	 * @param dir
	 * @param file
	 * @param type
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public ConfigFile(final String dirName, final String fileName, final Class<T> typeClass) throws IOException, InstantiationException, IllegalAccessException {
		final File dir = new File(System.getProperty("user.home"), dirName);
		if (!dir.exists() && !dir.mkdir()) {
			throw new IOException(dirName + " konnte nicht erstellt werden!");
		}

		file = new File(dir, fileName);
		type = typeClass;

		if (file.createNewFile()) {
			init();
		} else {
			read();
		}
	}

	/**
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public final synchronized void init() throws JsonGenerationException, JsonMappingException, IOException, InstantiationException, IllegalAccessException {
		data = type.newInstance();
		write();
	}

	/**
	 * 
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public final synchronized void read() throws IOException, InstantiationException, IllegalAccessException {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			data = mapper.readValue(file, type);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid config file.
			init();
		}
	}

	/**
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public final synchronized void write() throws JsonGenerationException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
	}
}
