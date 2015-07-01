package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.scribe.model.Token;

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
	public static Path openTreeFile() throws IOException {
		final Path configDir = Paths.get(System.getProperty("user.home"), CONFIG_DIR);
		if (!Files.isDirectory(configDir)) {
			Files.createDirectory(configDir);
		}

		return configDir.resolve(TREE_FILE_NAME);
	}

	/**
	 * Init oauth config file.
	 * 
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
		synchronized (settings) {
			return settings.data.rootDir;
		}
	}

	/**
	 * Set root directoy.
	 * 
	 * @throws IOException 
	 */
	public void setRootDirectory(final String rootDir) throws IOException {
		synchronized (settings) {
			settings.data.rootDir = rootDir;
			settings.write();
		}
	}

	/**
	 * Check if overwrite files setting is enabled.
	 */
	public boolean isOverwriteFiles() {
		synchronized (settings) {
			return settings.data.overwriteFiles;
		}
	}

	/**
	 * Set overwrite files setting.
	 * 
	 * @throws IOException 
	 */
	public void setOverwriteFiles(final boolean value) throws IOException {
		synchronized (settings) {
			settings.data.overwriteFiles = value;
			settings.write();
		}
	}

	/**
	 * Check if download all semesters setting is enabled.
	 */
	public boolean isDownloadAllSemesters() {
		synchronized (settings) {
			return settings.data.downloadAllSemesters;
		}
	}

	/**
	 * Set download all semesters setting.
	 * 
	 * @throws IOException 
	 */
	public void setDownloadAllSemesters(final boolean value) throws IOException {
		synchronized (settings) {
			settings.data.downloadAllSemesters = value;
			settings.write();
		}
	}

	/**
	 * Get replace whitespaces setting.
	 */
	public int getReplaceWhitespaces() {
		synchronized (settings) {
			return settings.data.replaceWhitespaces;
		}
	}

	/**
	 * Set replace whitespace setting.
	 * 
	 * @throws IOException 
	 */
	public void setReplaceWhitespaces(final int value) throws IOException {
		synchronized (settings) {
			settings.data.replaceWhitespaces = value;
			settings.write();
		}
	}

	/**
	 * Get logged in user firstname.
	 */
	public String getFirstName() {
		synchronized (oauth) {
			return oauth.data.firstName;
		}
	}

	/**
	 * Get logged in user lastname.
	 */
	public String getLastName() {
		synchronized (oauth) {
			return oauth.data.lastName;
		}
	}

	/**
	 * Get logged in user name.
	 */
	public String getUserName() {
		synchronized (oauth) {
			return oauth.data.userName;
		}
	}

	/**
	 * Get logged in user id.
	 */
	public String getUserId() {
		synchronized (oauth) {
			return oauth.data.userId;
		}
	}

	/**
	 * Get the OAuth access token.
	 * 
	 * @see OAuth.restoreAccessToken()
	 * @throws UnauthorizedException 
	 */
	public Token getAccessToken() throws UnauthorizedException {
		synchronized (oauth) {
			try {
				return new Token(oauth.data.token, oauth.data.secret);

			} catch (IllegalArgumentException e) {
				throw new UnauthorizedException(e.getMessage());
			}
		}
	}

	/**
	 * Set the OAuth access token.
	 * 
	 * @param accessToken
	 * @param currentUser
	 * @throws IOException 
	 */
	public void setAccessToken(final Token accessToken, final User currentUser) throws IOException {
		synchronized (oauth) {
			oauth.data.firstName = currentUser.forename;
			oauth.data.lastName = currentUser.lastname;
			oauth.data.userName = currentUser.username;
			oauth.data.userId = currentUser.user_id;
			oauth.data.token = accessToken.getToken();
			oauth.data.secret = accessToken.getSecret();
			oauth.write();
		}
	}
}
