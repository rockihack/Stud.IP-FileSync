package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import org.scribe.model.Token;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.uni.hannover.studip.sync.datamodel.ConfigFile;
import de.uni.hannover.studip.sync.datamodel.OAuthFile;

public class Config {
	
	/**
	 * Singleton instance.
	 */
	private static final Config singletonInstance = new Config();
	
	/**
	 * Global config file name.
	 */
	private static final String CONFIG_FILE_NAME = "config.json";
	
	/**
	 * OAuth config file name.
	 */
	private static final String OAUTH_FILE_NAME = "oauth.json";

	/**
	 * Global config file.
	 */
	private ConfigFile configFile;
	
	/**
	 * OAuth config file.
	 */
	private OAuthFile oauthFile;
	
	/**
	 * Singleton instance getter.
	 * 
	 * @return
	 */
	public static Config getInstance() {
		return singletonInstance;
	}
	
	/**
	 * Constructor.
	 */
	private Config() {
		try {
			readOAuthFile();
			readConfigFile();
			
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Get the user home directory.
	 * 
	 * @return
	 */
	public String getHomeDirectory() {
		String homePath = System.getProperty("user.home");
		return homePath;
	}
	
	/**
	 * Get the config directory.
	 * 
	 * @return
	 */
	public String getConfigDirectory() {
		String configPath = getHomeDirectory() + File.separator + ".studip-sync";
		return configPath;
	}



	/**
	 * Open global config file.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File openConfigFile() throws IOException {
		String configPath = getConfigDirectory();
		
		File configDir = new File(configPath);
		configDir.mkdir();
		
		File config = new File(configPath + File.separator + CONFIG_FILE_NAME);
		if (config.createNewFile()) {
			configFile = new ConfigFile();
			writeConfigFile();
		}
		
		return config;
	}
	
	/**
	 * Read global config file.
	 * 
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void readConfigFile() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		configFile = mapper.readValue(openConfigFile(), ConfigFile.class);
	}
	
	/**
	 * Write global config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void writeConfigFile() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		mapper.writeValue(openConfigFile(), configFile);
	}



	/**
	 * Open oauth config file.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File openOAuthFile() throws IOException {
		String configPath = getConfigDirectory();
		
		File configDir = new File(configPath);
		configDir.mkdir();
		
		File oauth = new File(configPath + File.separator + OAUTH_FILE_NAME);
		if (oauth.createNewFile()) {
			oauthFile = new OAuthFile();
			writeOAuthFile();
		}
		
		return oauth;
	}
	
	/**
	 * Read OAuth config file.
	 * 
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void readOAuthFile() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		oauthFile = mapper.readValue(openOAuthFile(), OAuthFile.class);
	}
	
	/**
	 * Write OAuth config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private void writeOAuthFile() throws JsonGenerationException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();

		mapper.writeValue(openOAuthFile(), oauthFile);
	}



	/**
	 * Get the OAuth access token.
	 * 
	 * @param accessToken
	 */
	public Token getAccessToken() {
		return new Token(oauthFile.token, oauthFile.secret);
	}
	
	/**
	 * Set the OAuth access token.
	 * 
	 * @param accessToken
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setAccessToken(Token accessToken) throws JsonGenerationException, JsonMappingException, IOException {
		oauthFile.token = accessToken.getToken();
		oauthFile.secret = accessToken.getSecret();
		
		writeOAuthFile();
	}
}
