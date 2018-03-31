package de.uni.hannover.studip.sync.models;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.scribe.model.Token;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.elanev.studip.android.app.backend.datamodel.User;
import de.uni.hannover.studip.sync.datamodel.SettingsFile;
import de.uni.hannover.studip.sync.datamodel.OAuthFile;

/**
 * Global config wrapper class.
 * 
 * @author Lennart Glauer
 * @notice Thread safe
 */
public final class Config {

	private static final Config INSTANCE = new Config();
	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String CONFIG_DIR = ".studip-sync";
	private static final String SETTINGS_FILE_NAME = "config.json";
	private static final String OAUTH_FILE_NAME = "oauth.json";
	private static final String TREE_FILE_NAME = "tree.json";

	private final ConfigFile<SettingsFile> settings;
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
	 * Jackson mapper instance getter.
	 * 
	 * @return
	 */
	public static ObjectMapper getMapper() {
		return MAPPER;
	}

	/**
	 * Constructor.
	 */
	private Config() {
		try {
			settings = new ConfigFile<>(CONFIG_DIR, SETTINGS_FILE_NAME, SettingsFile.class);
			oauth = new ConfigFile<>(CONFIG_DIR, OAUTH_FILE_NAME, OAuthFile.class);

		} catch (IOException e) {
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
	 * @throws IOException
	 */
	public void initOAuthFile() throws IOException {
		oauth.init();
	}

	/**
	 * Get root directoy.
	 */
	public String getRootDirectory() {
		settings.lock.readLock().lock();
		try {
			return settings.data.rootDir;

		} finally {
			settings.lock.readLock().unlock();
		}
	}

	/**
	 * Set root directoy.
	 * 
	 * @throws IOException 
	 */
	public void setRootDirectory(final String rootDir) throws IOException {
		settings.lock.writeLock().lock();
		try {
			settings.data.rootDir = rootDir;
			settings.write();

		} finally {
			settings.lock.writeLock().unlock();
		}
	}

	/**
	 * 
	 */
	public String getFolderStructure() {
		settings.lock.readLock().lock();
		try {
			return (settings.data.folderStructure == null || settings.data.folderStructure.isEmpty())
					? ":semester/:lecture/:type"
					: settings.data.folderStructure;

		} finally {
			settings.lock.readLock().unlock();
		}
	}

	/**
	 * 
	 * @throws IOException 
	 */
	public void setFolderStructure(final String template) throws IOException {
		settings.lock.writeLock().lock();
		try {
			settings.data.folderStructure = template;
			settings.write();

		} finally {
			settings.lock.writeLock().unlock();
		}
	}

	/**
	 * Check if overwrite files setting is enabled.
	 */
	public boolean isOverwriteFiles() {
		settings.lock.readLock().lock();
		try {
			return settings.data.overwriteFiles;

		} finally {
			settings.lock.readLock().unlock();
		}
	}

	/**
	 * Set overwrite files setting.
	 * 
	 * @throws IOException 
	 */
	public void setOverwriteFiles(final boolean value) throws IOException {
		settings.lock.writeLock().lock();
		try {
			settings.data.overwriteFiles = value;
			settings.write();

		} finally {
			settings.lock.writeLock().unlock();
		}
	}

	/**
	 * Check if download all semesters setting is enabled.
	 */
	public boolean isDownloadAllSemesters() {
		settings.lock.readLock().lock();
		try {
			return settings.data.downloadAllSemesters;

		} finally {
			settings.lock.readLock().unlock();
		}
	}

	/**
	 * Set download all semesters setting.
	 * 
	 * @throws IOException 
	 */
	public void setDownloadAllSemesters(final boolean value) throws IOException {
		settings.lock.writeLock().lock();
		try {
			settings.data.downloadAllSemesters = value;
			settings.write();

		} finally {
			settings.lock.writeLock().unlock();
		}
	}

	/**
	 * Get replace whitespaces setting.
	 */
	public int getReplaceWhitespaces() {
		settings.lock.readLock().lock();
		try {
			return settings.data.replaceWhitespaces;

		} finally {
			settings.lock.readLock().unlock();
		}
	}

	/**
	 * Set replace whitespace setting.
	 * 
	 * @throws IOException 
	 */
	public void setReplaceWhitespaces(final int value) throws IOException {
		settings.lock.writeLock().lock();
		try {
			settings.data.replaceWhitespaces = value;
			settings.write();

		} finally {
			settings.lock.writeLock().unlock();
		}
	}

	/**
	 * Get logged in user firstname.
	 */
	public String getFirstName() {
		oauth.lock.readLock().lock();
		try {
			return oauth.data.firstName;

		} finally {
			oauth.lock.readLock().unlock();
		}
	}

	/**
	 * Get logged in user lastname.
	 */
	public String getLastName() {
		oauth.lock.readLock().lock();
		try {
			return oauth.data.lastName;

		} finally {
			oauth.lock.readLock().unlock();
		}
	}

	/**
	 * Get logged in user name.
	 */
	public String getUserName() {
		oauth.lock.readLock().lock();
		try {
			return oauth.data.userName;

		} finally {
			oauth.lock.readLock().unlock();
		}
	}

	/**
	 * Get logged in user id.
	 */
	public String getUserId() {
		oauth.lock.readLock().lock();
		try {
			return oauth.data.userId;

		} finally {
			oauth.lock.readLock().unlock();
		}
	}

	/**
	 * Get the OAuth access token.
	 * 
	 * @see OAuth.restoreAccessToken()
	 * @throws IllegalArgumentException 
	 */
	public Token getAccessToken() {
		oauth.lock.readLock().lock();
		try {
			return new Token(oauth.data.token, oauth.data.secret);

		} finally {
			oauth.lock.readLock().unlock();
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
		oauth.lock.writeLock().lock();
		try {
			oauth.data.firstName = currentUser.name.given;
			oauth.data.lastName = currentUser.name.family;
			oauth.data.userName = currentUser.username;
			oauth.data.userId = currentUser.user_id;
			oauth.data.token = accessToken.getToken();
			oauth.data.secret = accessToken.getSecret();
			oauth.write();

		} finally {
			oauth.lock.writeLock().unlock();
		}
	}
}
