package de.uni.hannover.studip.sync.models;

import java.io.File;
import java.io.IOException;

import org.scribe.model.Token;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.datamodel.SettingsFile;
import de.uni.hannover.studip.sync.datamodel.OAuthFile;
import de.uni.hannover.studip.sync.exceptions.UnauthorizedException;

/**
 * Global config wrapper class.
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
	 * Config directory name.
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
	public void initOAuthFile() throws IOException, InstantiationException, IllegalAccessException {
		oauth.init();
	}

	/**
	 * Get root directoy.
	 */
	public String getRootDirectory() {
		return settings.data.rootDir;
	}

	/**
	 * Set root directoy.
	 * 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setRootDirectory(final String rootDir) throws IOException {
		settings.data.rootDir = rootDir;
		settings.write();
	}

	/**
	 * Check if overwrite files setting is enabled.
	 */
	public boolean isOverwriteFiles() {
		return settings.data.overwriteFiles;
	}

	/**
	 * Set overwrite files setting.
	 * 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setOverwriteFiles(final boolean value) throws IOException {
		settings.data.overwriteFiles = value;
		settings.write();
	}

	/**
	 * Check if download all semesters setting is enabled.
	 */
	public boolean isDownloadAllSemesters() {
		return settings.data.downloadAllSemesters;
	}

	/**
	 * Set download all semesters setting.
	 * 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setDownloadAllSemesters(final boolean value) throws IOException {
		settings.data.downloadAllSemesters = value;
		settings.write();
	}

	/**
	 * Get replace whitespaces setting.
	 */
	public int getReplaceWhitespaces() {
		return settings.data.replaceWhitespaces;
	}

	/**
	 * Set replace whitespace setting.
	 * 
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonGenerationException 
	 */
	public void setReplaceWhitespaces(final int value) throws IOException {
		settings.data.replaceWhitespaces = value;
		settings.write();
	}

	/**
	 * Get logged in user firstname.
	 */
	public String getFirstName() {
		return oauth.data.firstName;
	}

	/**
	 * Get logged in user lastname.
	 */
	public String getLastName() {
		return oauth.data.lastName;
	}

	/**
	 * Get logged in user name.
	 */
	public String getUserName() {
		return oauth.data.userName;
	}

	/**
	 * Get logged in user id.
	 */
	public String getUserId() {
		return oauth.data.userId;
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
	 * @throws IOException 
	 */
	public void setAccessToken(final Token accessToken, final User currentUser) throws IOException {
		oauth.data.firstName = currentUser.forename;
		oauth.data.lastName = currentUser.lastname;
		oauth.data.userName = currentUser.username;
		oauth.data.userId = currentUser.user_id;
		oauth.data.token = accessToken.getToken();
		oauth.data.secret = accessToken.getSecret();
		oauth.write();
	}
}
