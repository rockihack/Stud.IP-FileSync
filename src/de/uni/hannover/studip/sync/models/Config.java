package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import org.scribe.model.Token;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.datamodel.SettingsFile;
import de.uni.hannover.studip.sync.datamodel.OAuthFile;
import de.uni.hannover.studip.sync.exceptions.NotFoundException;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;

/**
 * 
 * @author Lennart Glauer
 *
 */
public final class Config {
	
	/**
	 * Singleton instance.
	 */
	private static final Config INSTANCE = new Config();
	
	/**
	 * Config directory.
	 */
	private static final String CONFIG_DIR = ".studip-sync";
	
	/**
	 * Config file name.
	 */
	private static final String SETTINGS_FILE_NAME = "config.json";
	
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
	private final ConfigFile<SettingsFile> settings;
	
	/**
	 * OAuth config file.
	 */
	private final ConfigFile<OAuthFile> oauth;
	
	/**
	 * Singleton instance getter.
	 * 
	 * @return
	 */
	public static Config getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Constructor.
	 */
	private Config() {
		try {
			settings = new ConfigFile<SettingsFile>(CONFIG_DIR, SETTINGS_FILE_NAME, SettingsFile.class);
			oauth = new ConfigFile<OAuthFile>(CONFIG_DIR, OAUTH_FILE_NAME, OAuthFile.class);

		} catch (InstantiationException | IllegalAccessException | IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Open tree file.
	 * 
	 * @return
	 * @throws IOException
	 */
	public static File openTreeFile() throws IOException {
		final File configDir = new File(System.getProperty("user.home"), CONFIG_DIR);
		if (!configDir.exists() && !configDir.mkdir()) {
			throw new IOException("Config Order konnte nicht erstellt werden!");
		}

		final File treeFile = new File(configDir, TREE_FILE_NAME);
		if (!treeFile.exists() && !treeFile.createNewFile()) {
			throw new IOException("Dateibaum konnte nicht erstellt werden!");
		}

		return treeFile;
	}

	/**
	 * Init oauth config file.
	 * 
	 * @throws JsonGenerationException
	 * @throws JsonMappingException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IOException
	 */
	public void initOAuthFile() throws JsonGenerationException, JsonMappingException, InstantiationException, IllegalAccessException, IOException {
		oauth.init();
	}

	/**
	 * Get root directoy.
	 */
	public String getRootDirectory() {
		return settings.data.root_dir;
	}

	/**
	 * Set root directoy.
	 * 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setRootDirectory(final String rootDir) throws JsonGenerationException, JsonMappingException, IOException {
		settings.data.root_dir = rootDir;
		settings.write();
	}

	/**
	 * 
	 */
	public boolean isOverwriteFiles() {
		return settings.data.overwrite_files;
	}

	/**
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * 
	 */
	public void setOverwriteFiles(final boolean value) throws JsonGenerationException, JsonMappingException, IOException {
		settings.data.overwrite_files = value;
		settings.write();
	}

	/**
	 * 
	 */
	public boolean isDownloadAllSemesters() {
		return settings.data.download_all_semesters;
	}

	/**
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * 
	 */
	public void setDownloadAllSemesters(final boolean value) throws JsonGenerationException, JsonMappingException, IOException {
		settings.data.download_all_semesters = value;
		settings.write();
	}

	/**
	 * 
	 */
	public int getReplaceWhitespaces() {
		return settings.data.replaceWhitespaces;
	}

	/**
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 * 
	 */
	public void setReplaceWhitespaces(final int value) throws JsonGenerationException, JsonMappingException, IOException {
		settings.data.replaceWhitespaces = value;
		settings.write();
	}

	/**
	 * Get logged in user firstname.
	 */
	public String getFirstName() {
		return oauth.data.first_name;
	}

	/**
	 * Get logged in user lastname.
	 */
	public String getLastName() {
		return oauth.data.last_name;
	}

	/**
	 * Get logged in user name.
	 */
	public String getUserName() {
		return oauth.data.user_name;
	}

	/**
	 * Get logged in user id.
	 */
	public String getUserId() {
		return oauth.data.user_id;
	}

	/**
	 * Get the OAuth access token.
	 * 
	 * @see OAuth.restoreAccessToken()
	 * @param accessToken
	 * @throws UnauthorizedException 
	 */
	public Token getAccessToken() throws UnauthorizedException {
		try {
			return new Token(oauth.data.token, oauth.data.secret);

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
	public void setAccessToken(final Token accessToken, final User currentUser) throws UnauthorizedException, NotFoundException, IOException {
		oauth.data.first_name = currentUser.forename;
		oauth.data.last_name = currentUser.lastname;
		oauth.data.user_name = currentUser.username;
		oauth.data.user_id = currentUser.user_id;
		oauth.data.token = accessToken.getToken();
		oauth.data.secret = accessToken.getSecret();
		oauth.write();
	}
}
