package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Jackson request.
 * 
 * @param <T> Response data model.
 * @author Lennart Glauer
 */
public class JacksonRequest<T> {
	
	/**
	 * OAuth service.
	 */
	private static final OAuth OAUTH = OAuth.getInstance();
	
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
	 * 
	 * @param method
	 * @param url
	 * @param datamodel
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
	 * @return Response object
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public T parseResponse(final boolean unwrap) throws JsonParseException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		
		/* Unwrap root value. */
		if (unwrap) {
			mapper.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);
		}
		
		return mapper.readValue(response.getBody(), datamodel);
	}

	/**
	 * Get request method.
	 * 
	 * @return HTTP method
	 */
	public Verb getMethod() {
		return method;
	}
	
	/**
	 * Get request url.
	 * 
	 * @return Url
	 */
	public String getUrl() {
		return url;
	}
	
	/**
	 * Get response status code.
	 * 
	 * @return HTTP status code
	 */
	public int getCode() {
		return response.getCode();
	}
	
	/**
	 * Get response headers.
	 * 
	 * @return Header map
	 */
	public Map<String, String> getHeaders() {
		return response.getHeaders();
	}
	
	/**
	 * Get input stream.
	 * 
	 * @return
	 */
	public InputStream getStream() {
		return response.getStream();
	}
	
}
