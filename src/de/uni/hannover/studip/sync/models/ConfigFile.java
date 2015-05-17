package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Config file wrapper class.
 * 
 * @author Lennart Glauer
 *
 * @param <T> Config datamodel
 */
public final class ConfigFile<T> {

	private final File file;
	private final Class<T> type;

	public T data;

	/**
	 * Open and read config file.
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
	 * Init config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public synchronized void init() throws JsonGenerationException, JsonMappingException, IOException, InstantiationException, IllegalAccessException {
		data = type.newInstance();
		write();
	}

	/**
	 * Read config file.
	 * 
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public synchronized void read() throws IOException, InstantiationException, IllegalAccessException {
		try {
			final ObjectMapper mapper = new ObjectMapper();
			data = mapper.readValue(file, type);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid config file.
			init();
		}
	}

	/**
	 * Write config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public synchronized void write() throws JsonGenerationException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
	}
}
