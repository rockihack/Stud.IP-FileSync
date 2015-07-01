package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

	private final Path file;
	private final Class<T> type;

	public T data;

	/**
	 * Open and read config file.
	 * 
	 * @param dir Directory name
	 * @param file File name
	 * @param type Datamodel class
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public ConfigFile(final String dirName, final String fileName, final Class<T> typeClass) throws IOException, InstantiationException, IllegalAccessException {
		final Path dir = Paths.get(System.getProperty("user.home"), dirName);
		if (!Files.isDirectory(dir)) {
			Files.createDirectory(dir);
		}

		file = dir.resolve(fileName);
		type = typeClass;

		if (Files.exists(file)) {
			read();
		} else {
			init();
		}
	}

	/**
	 * Init config file.
	 * 
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public synchronized void init() throws IOException, InstantiationException, IllegalAccessException {
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
			data = mapper.readValue(Files.newBufferedReader(file), type);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid config file.
			init();
		}
	}

	/**
	 * Write config file.
	 * 
	 * @throws IOException
	 */
	public synchronized void write() throws IOException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(Files.newBufferedWriter(file), data);
	}
}
