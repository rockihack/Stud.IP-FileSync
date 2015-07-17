package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

	/**
	 * Path to file.
	 */
	private final Path file;

	/**
	 * Datamodel class.
	 */
	private final Class<T> datamodel;

	/**
	 * Reentrant read/write lock.
	 */
	public final ReentrantReadWriteLock lock;

	/**
	 * Data.
	 */
	public T data;

	/**
	 * Open and read config file.
	 * 
	 * @param dirName Directory name
	 * @param fileName File name
	 * @param datamodelClass Datamodel class
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public ConfigFile(final String dirName, final String fileName, final Class<T> datamodelClass) throws IOException, InstantiationException, IllegalAccessException {
		final Path dir = Paths.get(System.getProperty("user.home"), dirName);
		if (!Files.isDirectory(dir)) {
			Files.createDirectory(dir);
		}

		file = dir.resolve(fileName);
		datamodel = datamodelClass;
		lock = new ReentrantReadWriteLock();

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
	public void init() throws IOException, InstantiationException, IllegalAccessException {
		lock.writeLock().lock();
		try {
			data = datamodel.newInstance();
			write();

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Read config file.
	 * 
	 * @throws IOException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	public void read() throws IOException, InstantiationException, IllegalAccessException {
		lock.writeLock().lock();
		try {
			final ObjectMapper mapper = new ObjectMapper();
			data = mapper.readValue(Files.newBufferedReader(file), datamodel);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid config file.
			init();

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Write config file.
	 * 
	 * @throws IOException
	 */
	public void write() throws IOException {
		lock.writeLock().lock();
		try {
			final ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(Files.newBufferedWriter(file), data);

		} finally {
			lock.writeLock().unlock();
		}
	}
}
