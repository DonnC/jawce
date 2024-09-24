package zw.co.dcl.jawce.session.impl;

import zw.co.dcl.jawce.session.Constants;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.session.utils.SessionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static zw.co.dcl.jawce.session.Constants.*;

/**
 * A file-based session manager that uses synchronized methods
 * <p>
 * each phone number becomes the file name with .sessions ext
 * <p>
 * all global data are saved in the global.sessions file
 */
public class FileBasedSessionManager implements ISessionManager {
    private final static Logger LOGGER = Logger.getLogger(FileBasedSessionManager.class.getName());
    private static volatile FileBasedSessionManager instance;

    private final Path SESSION_DIR;
    private final Path GLOBAL_SESSION_FILE;

    // Private constructor to enforce Singleton pattern
    private FileBasedSessionManager() {
        try {
            this.SESSION_DIR = Paths.get(FILE_FOLDER).toAbsolutePath().normalize();
            Files.createDirectories(this.SESSION_DIR);
            this.GLOBAL_SESSION_FILE = this.SESSION_DIR.resolve(GLOBAL_SESSION_FILE_NAME).toAbsolutePath().normalize();
            createFileIfNotExist(GLOBAL_SESSION_FILE.toFile());
            LOGGER.info("File based session manager initialized!");
        } catch (Exception e) {
            throw new RuntimeException("Cannot create sessions directory: " + e.getMessage());
        }
    }

    // Method to get the Singleton instance
    public static FileBasedSessionManager getInstance() {
        if(instance == null) {
            synchronized (FileBasedSessionManager.class) {
                if(instance == null) {
                    instance = new FileBasedSessionManager();
                }
            }
        }
        return instance;
    }

    private Path getUserSessionFile(String user) {
        return this.SESSION_DIR.resolve(user.trim() + SESSION_FILE_EXT).toAbsolutePath().normalize();
    }

    private void createFileIfNotExist(File file) {
        if(!file.exists()) {
            SessionUtils.writeToFile(file, new HashMap<>());
        }
    }

    @Override
    public ISessionManager session(String user) {
        if(user == null) return this;

        var userFile = getUserSessionFile(user).toFile();
        createFileIfNotExist(userFile);

        return this;
    }

    @Override
    public synchronized void save(String user, String key, Object data) {
        var sessionData = loadSessionData(user);
        sessionData.put(key, data);
        saveSessionData(user, sessionData);
    }

    @Override
    public void saveGlobal(String key, Object data) {
        save(null, key, data);
    }

    @Override
    public synchronized void saveProp(String user, String propKey, Object data) {
        var sessionData = loadSessionData(user);
        var props = (Map<String, Object>) sessionData.get(Constants.USER_PROPS_KEY);

        if(props == null) {
            props = new HashMap<>();
        }

        props.put(propKey, data);
        sessionData.put(Constants.USER_PROPS_KEY, props);
        saveSessionData(user, sessionData);
    }

    @Override
    public synchronized boolean evictProp(String user, String propKey) {
        try {
            var sessionData = loadSessionData(user);
            var props = (Map<String, Object>) sessionData.get(Constants.USER_PROPS_KEY);
            if(props != null && props.containsKey(propKey)) {
                props.remove(propKey);
                sessionData.put(Constants.USER_PROPS_KEY, props);
                saveSessionData(user, sessionData);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object getFromProps(String user, String propKey) {
        try {
            var sessionData = loadSessionData(user);
            var props = (Map<String, Object>) sessionData.get(Constants.USER_PROPS_KEY);
            return (props != null) ? props.get(propKey) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T getFromProps(String user, String propKey, Class<T> propType) {
        Object propValue = getFromProps(user, propKey);
        return (propValue != null) ? propType.cast(propValue) : null;
    }

    @Override
    public Object get(String user, String key) {
        return loadSessionData(user).get(key);
    }

    @Override
    public <T> T get(String user, String key, Class<T> type) {
        return SessionUtils.castValue(loadSessionData(user).get(key), type);
    }

    @Override
    public <T> T getGlobal(String key, Class<T> type) {
        return get(null, key, type);
    }

    @Override
    public Map<String, Object> fetchAll(String user) {
        return loadSessionData(user);
    }

    @Override
    public synchronized void evict(String user, String key) {
        Map<String, Object> sessionData = loadSessionData(user);
        sessionData.remove(key);
        saveSessionData(user, sessionData);
    }

    @Override
    public void evictGlobal(String key) {
        evict(null, key);
    }

    @Override
    public void clear(String user) {
        saveSessionData(user, new HashMap<>());
    }

    @Override
    public boolean keyInSession(String user, String key, boolean global) {
        var data = loadSessionData(global ? null : user);
        return data.containsKey(key);
    }

    @Override
    public void clear(String user, List<String> retain) {
        List<String> retainKeys = new ArrayList<>(retain);

        if(retainKeys.isEmpty()) return;

        // collect keys first and then evict to avoid ConcurrentModificationException
        List<String> keysToEvict = new ArrayList<>();

        fetchAll(user).forEach((k, v) -> {
            if(!retainKeys.contains(k)) {
                keysToEvict.add(k);
            }
        });

        for (String key : keysToEvict) {
            evict(user, key);
        }
    }

    @Override
    public void saveAll(String user, Map<String, Object> sessionData) {
        sessionData.forEach((key, value) -> save(user, key, value));
    }

    @Override
    public void evictAll(String user, List<String> keys) {
        keys.forEach(key -> evict(user, key));
    }

    @Override
    public Map<String, Object> getUserProps(String user) {
        var props = this.get(user, Constants.USER_PROPS_KEY, Map.class);
        return Objects.requireNonNullElseGet(props, HashMap::new);
    }

    private synchronized Map<String, Object> loadSessionData(String user) {
        var sessionPath = user == null ? GLOBAL_SESSION_FILE : getUserSessionFile(user);
        return SessionUtils.readMapFromFile(sessionPath.toFile());
    }

    private synchronized void saveSessionData(String user, Map<String, Object> sessionData) {
        var sessionPath = user == null ? GLOBAL_SESSION_FILE : getUserSessionFile(user);
        SessionUtils.writeToFile(sessionPath.toFile(), sessionData);
    }

    public void deleteSessionFolder() {
        try {
            SessionUtils.deleteDirectoryRecursively(this.SESSION_DIR);
            LOGGER.info("File manager directory deleted");
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file session folder", e);
        }
    }
}
