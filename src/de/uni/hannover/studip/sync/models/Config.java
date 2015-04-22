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
	 * Config directory.
	 */
	private static final String CONFIG_DIR = ".studip-sync";
	
	/**
	 * Config file name.
	 */
	private static final String CONFIG_FILE_NAME = "config.json";
	
	/**
	 * OAuth config file name.
	 */
	private static final String OAUTH_FILE_NAME = "oauth.json";
	
	/**
	 * Tree file name.
	 */
	private static final String TREE_FILE_NAME = "tree.json";

	/**
	 * Global config file.
	 */
	private ConfigFile config;
	
	/**
	 * OAuth config file.
	 */
	private OAuthFile oauth;
	
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
		return System.getProperty("user.home");
	}

	/**
	 * Open global config file.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File openConfigFile() throws IOException {
		File configDir = new File(getHomeDirectory(), CONFIG_DIR);
		configDir.mkdir();
		
		File configFile = new File(configDir, CONFIG_FILE_NAME);
		if (configFile.createNewFile()) {
			config = new ConfigFile();
			writeConfigFile();
		}
		
		return configFile;
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
		config = mapper.readValue(openConfigFile(), ConfigFile.class);
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
		mapper.writeValue(openConfigFile(), config);
	}

	/**
	 * Open oauth config file.
	 * 
	 * @return
	 * @throws IOException
	 */
	private File openOAuthFile() throws IOException {
		File configDir = new File(getHomeDirectory(), CONFIG_DIR);
		configDir.mkdir();
		
		File oauthFile = new File(configDir, OAUTH_FILE_NAME);
		if (oauthFile.createNewFile()) {
			oauth = new OAuthFile();
			writeOAuthFile();
		}
		
		return oauthFile;
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
		oauth = mapper.readValue(openOAuthFile(), OAuthFile.class);
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
		mapper.writeValue(openOAuthFile(), oauth);
	}
	
	/**
	 * Open tree file.
	 * 
	 * @return
	 * @throws IOException
	 */
	public File openTreeFile() throws IOException {
		File configDir = new File(getHomeDirectory(), CONFIG_DIR);
		configDir.mkdir();
		
		File treeFile = new File(configDir, TREE_FILE_NAME);
		treeFile.createNewFile();
		
		return treeFile;
	}

	/**
	 * Get root directoy.
	 */
	public String getRootDirectory() {
		return config.root_dir;
	}
	
	/**
	 * Get root directoy.
	 */
	public boolean getRenameModifiedFiles() {
		return config.rename_modified_files;
	}

	/**
	 * Get the OAuth access token.
	 * 
	 * @param accessToken
	 */
	public Token getAccessToken() {
		return oauth.token != null && oauth.secret != null
				? new Token(oauth.token, oauth.secret)
				: null;
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
		oauth.token = accessToken.getToken();
		oauth.secret = accessToken.getSecret();

		writeOAuthFile();
	}
}
