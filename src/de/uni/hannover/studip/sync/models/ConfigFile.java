package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigFile<T> {

	private final File dir;
	private final File file;
	private final Class<T> type;

	public T data;

	public ConfigFile(String dir, String file, Class<T> type) throws IOException, InstantiationException, IllegalAccessException {
		this.dir = new File(Config.getHomeDirectory(), dir);
		this.dir.mkdir();

		this.file = new File(this.dir, file);
		this.type = type;

		if (this.file.createNewFile()) {
			init();
		} else {
			read();
		}
	}

	public synchronized void init() throws JsonGenerationException, JsonMappingException, IOException, InstantiationException, IllegalAccessException {
		data = type.newInstance();
		write();
	}

	public synchronized void read() throws IOException, InstantiationException, IllegalAccessException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			data = mapper.readValue(file, type);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid config file.
			init();
		}
	}

	public synchronized void write() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(file, data);
	}
}
