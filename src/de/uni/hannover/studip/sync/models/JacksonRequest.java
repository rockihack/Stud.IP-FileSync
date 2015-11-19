package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Jackson request.
 * 
 * @author Lennart Glauer
 * @notice Thread safe (immutable object)
 * @param <T> Response data model.
 */
public class JacksonRequest<T> {

	private static final OAuth OAUTH = OAuth.getInstance();
	private static final ObjectMapper MAPPER = Config.getMapper();

	/**
	 * Request method.
	 */
	private final Verb method;

	/**
	 * Request url.
	 */
	private final String url;

	/**
	 * Request data model.
	 */
	private final Class<T> datamodel;

	/**
	 * OAuth response.
	 */
	private final Response response;

	/**
	 * Send jackson request.
	 * 
	 * @param method Request method
	 * @param url Request url
	 * @param datamodel Datamodel class
	 */
	public JacksonRequest(final Verb method, final String url, final Class<T> datamodel) {
		this.method = method;
		this.url = url;
		this.datamodel = datamodel;

		/* Send rest api request using oauth service. */
		this.response = OAUTH.sendRequest(method, url);
	}

	/**
	 * Parse response into data model object.
	 * 
	 * @param unwrap Unwrap datamodel
	 * @return Response datamodel
	 * @throws IOException
	 */
	public T parseResponse() throws IOException {
		ObjectReader reader = MAPPER.readerFor(datamodel).without(Feature.AUTO_CLOSE_SOURCE);
		if (datamodel.isAnnotationPresent(JsonRootName.class)) {
			reader = reader.with(DeserializationFeature.UNWRAP_ROOT_VALUE);
		}

		try (final InputStream is = response.getStream()) {
			final T result = reader.readValue(is);

			/*
			 * ObjectReader.readValue might not consume the entire inputstream,
			 * we skip everything after the json root element.
			 * This is needed for proper http connection reuse (keep alive).
			 */
			is.skip(Long.MAX_VALUE);

			return result;
		}
	}

	/**
	 * Get request method.
	 * 
	 * @return Request HTTP method
	 */
	public Verb getMethod() {
		return method;
	}

	/**
	 * Get request url.
	 * 
	 * @return Request url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Get response status code.
	 * 
	 * @return Response HTTP status code
	 */
	public int getCode() {
		return response.getCode();
	}

	/**
	 * Get response headers.
	 * 
	 * @return Response header map
	 */
	public Map<String, String> getHeaders() {
		return response.getHeaders();
	}

	/**
	 * Get input stream.
	 * 
	 * @return Response input stream
	 */
	public InputStream getStream() {
		return response.getStream();
	}
}