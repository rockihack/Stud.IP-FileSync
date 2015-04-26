package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import org.scribe.model.Token;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.datamodel.ConfigFile;
import de.uni.hannover.studip.sync.datamodel.OAuthFile;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;

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
			readConfigFile();
			readOAuthFile();

		} catch (IOException e) {
			throw new IllegalStateException(e);
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
			initConfigFile();
		}
		
		return configFile;
	}

	protected synchronized void initConfigFile() throws JsonGenerationException, JsonMappingException, IOException {
		config = new ConfigFile();
		writeConfigFile();
	}
	
	/**
	 * Read global config file.
	 * 
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private synchronized void readConfigFile() throws IOException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			config = mapper.readValue(openConfigFile(), ConfigFile.class);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid config file.
			try {
				initConfigFile();
			} catch (IOException e1) {
				throw new IllegalStateException(e1);
			}
		}
	}
	
	/**
	 * Write global config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private synchronized void writeConfigFile() throws JsonGenerationException, JsonMappingException, IOException {
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
			initOAuthFile();
		}
		
		return oauthFile;
	}
	
	protected synchronized void initOAuthFile() throws JsonGenerationException, JsonMappingException, IOException {
		oauth = new OAuthFile();
		writeOAuthFile();
	}

	/**
	 * Read OAuth config file.
	 * 
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private synchronized void readOAuthFile() throws IOException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			oauth = mapper.readValue(openOAuthFile(), OAuthFile.class);

		} catch (JsonParseException | JsonMappingException e) {
			// Invalid oauth config file.
			try {
				initOAuthFile();
			} catch (IOException e1) {
				throw new IllegalStateException(e1);
			}
		}
	}
	
	/**
	 * Write OAuth config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	private synchronized void writeOAuthFile() throws JsonGenerationException, JsonMappingException, IOException {
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
	 * Set root directoy.
	 * 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setRootDirectory(String root_dir) throws JsonGenerationException, JsonMappingException, IOException {
		config.root_dir = root_dir;

		writeConfigFile();
	}

	/**
	 * 
	 */
	public boolean getOverwriteFiles() {
		return config.overwrite_files;
	}

	/**
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * 
	 */
	public void setOverwriteFiles(boolean value) throws JsonGenerationException, JsonMappingException, IOException {
		config.overwrite_files = value;

		writeConfigFile();
	}

	/**
	 * Get logged in user firstname.
	 */
	public String getFirstName() {
		return oauth.first_name;
	}

	/**
	 * Get logged in user lastname.
	 */
	public String getLastName() {
		return oauth.last_name;
	}

	/**
	 * Get logged in user name.
	 */
	public String getUserName() {
		return oauth.user_name;
	}

	/**
	 * Get logged in user id.
	 */
	public String getUserId() {
		return oauth.user_id;
	}

	/**
	 * Get the OAuth access token.
	 * 
	 * @see OAuth.restoreAccessToken()
	 * @param accessToken
	 * @throws UnauthorizedException 
	 */
	protected Token getAccessToken() throws UnauthorizedException {
		try {
			return new Token(oauth.token, oauth.secret);

		} catch (IllegalArgumentException e) {
			throw new UnauthorizedException(e.getMessage());
		}
	}
	
	/**
	 * Set the OAuth access token.
	 * 
	 * @param accessToken
	 * @throws NotFoundException 
	 * @throws UnauthorizedException 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	protected void setAccessToken(Token accessToken) throws UnauthorizedException, NotFoundException, IOException {
		// Test if access token is valid.
		User current_user = RestApi.getUserById(null);

		oauth.first_name = current_user.forename;
		oauth.last_name = current_user.lastname;
		oauth.user_name = current_user.username;
		oauth.user_id = current_user.user_id;
		oauth.token = accessToken.getToken();
		oauth.secret = accessToken.getSecret();

		writeOAuthFile();
	}
}
