package zw.co.dcl.ehailing.service.engine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A file-based session manager that uses synchronized methods
 * <p>
 * each phone number becomes the file name with .sessions ext
 * <p>
 * all global data are saved in the global.sessions file
 */
@Slf4j
public class FileSessionManager implements ISessionManager {
    private static volatile FileSessionManager instance;
    private final String USER_PROPS_KEY = "jProps";
    private final String SESSION_FILE_EXT = ".session";

    @Getter
    private final Path SESSION_DIR;
    @Getter
    private final Path GLOBAL_SESSION_FILE;

    private FileSessionManager() {
        try {
            this.SESSION_DIR = Paths.get("./.session").toAbsolutePath().normalize();
            Files.createDirectories(this.SESSION_DIR);
            var GLOBAL_SESSION_FILE_NAME = "global" + SESSION_FILE_EXT;
            this.GLOBAL_SESSION_FILE = this.SESSION_DIR.resolve(GLOBAL_SESSION_FILE_NAME).toAbsolutePath().normalize();
            createFileIfNotExist(GLOBAL_SESSION_FILE.toFile());
            log.info("File based session manager initialized!");
        } catch (Exception e) {
            throw new RuntimeException("Cannot create sessions directory: " + e.getMessage());
        }
    }

    // Method to get the Singleton instance
    public static FileSessionManager getInstance() {
        if(instance == null) {
            synchronized (FileSessionManager.class) {
                if(instance == null) {
                    instance = new FileSessionManager();
                }
            }
        }
        return instance;
    }

    private Path getUserSessionFile(String sessionId) {
        return this.SESSION_DIR.resolve(sessionId + SESSION_FILE_EXT).toAbsolutePath().normalize();
    }

    private void createFileIfNotExist(File file) {
        if(!file.exists()) {
            SerializeUtils.writeToFile(file, new HashMap<>());
        }
    }

    @Override
    public ISessionManager session(String sessionId) {
        if(sessionId == null) return this;

        var userFile = getUserSessionFile(sessionId).toFile();
        createFileIfNotExist(userFile);

        return this;
    }

    @Override
    public synchronized void save(String sessionId, String key, Object data) {
        var sessionData = loadSessionData(sessionId);
        sessionData.put(key, data);
        saveSessionData(sessionId, sessionData);
    }

    @Override
    public void saveGlobal(String key, Object data) {
        save(null, key, data);
    }

    @Override
    public synchronized void saveProp(String sessionId, String propKey, Object data) {
        var sessionData = loadSessionData(sessionId);
        var props = (Map<String, Object>) sessionData.get(USER_PROPS_KEY);

        if(props == null) {
            props = new HashMap<>();
        }

        props.put(propKey, data);
        sessionData.put(USER_PROPS_KEY, props);
        saveSessionData(sessionId, sessionData);
    }

    @Override
    public synchronized boolean evictProp(String sessionId, String propKey) {
        try {
            var sessionData = loadSessionData(sessionId);
            var props = (Map<String, Object>) sessionData.get(USER_PROPS_KEY);
            if(props != null && props.containsKey(propKey)) {
                props.remove(propKey);
                sessionData.put(USER_PROPS_KEY, props);
                saveSessionData(sessionId, sessionData);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object getFromProps(String sessionId, String propKey) {
        try {
            var sessionData = loadSessionData(sessionId);
            var props = (Map<String, Object>) sessionData.get(USER_PROPS_KEY);
            return (props != null) ? props.get(propKey) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T getFromProps(String sessionId, String propKey, Class<T> propType) {
        Object propValue = getFromProps(sessionId, propKey);
        return (propValue != null) ? propType.cast(propValue) : null;
    }

    @Override
    public Object get(String sessionId, String key) {
        return loadSessionData(sessionId).get(key);
    }

    @Override
    public <T> T get(String sessionId, String key, Class<T> type) {
        return SerializeUtils.castValue(loadSessionData(sessionId).get(key), type);
    }

    @Override
    public <T> T getGlobal(String key, Class<T> type) {
        return get(null, key, type);
    }

    @Override
    public Map<String, Object> fetchAll(String sessionId) {
        return loadSessionData(sessionId);
    }

    @Override
    public synchronized void evict(String sessionId, String key) {
        Map<String, Object> sessionData = loadSessionData(sessionId);
        sessionData.remove(key);
        saveSessionData(sessionId, sessionData);
    }

    @Override
    public void evictGlobal(String key) {
        evict(null, key);
    }

    @Override
    public void clear(String sessionId) {
        saveSessionData(sessionId, new HashMap<>());
    }

    @Override
    public boolean keyInSession(String sessionId, String key, boolean global) {
        var data = loadSessionData(global ? null : sessionId);
        return data.containsKey(key);
    }

    @Override
    public void clear(String sessionId, List<String> retain) {
        List<String> retainKeys = new ArrayList<>(retain);

        if(retainKeys.isEmpty()) return;

        List<String> keysToEvict = new ArrayList<>();

        fetchAll(sessionId).forEach((k, v) -> {
            if(!retainKeys.contains(k)) {
                keysToEvict.add(k);
            }
        });

        evictAll(sessionId, keysToEvict);
    }

    @Override
    public void saveAll(String sessionId, Map<String, Object> sessionData) {
        sessionData.forEach((key, value) -> save(sessionId, key, value));
    }

    @Override
    public void evictAll(String sessionId, List<String> keys) {
        keys.forEach(key -> evict(sessionId, key));
    }

    @Override
    public Map<String, Object> getUserProps(String sessionId) {
        var props = this.get(sessionId, USER_PROPS_KEY, Map.class);
        return Objects.requireNonNullElseGet(props, HashMap::new);
    }

    private synchronized Map<String, Object> loadSessionData(String sessionId) {
        var sessionPath = sessionId == null ? GLOBAL_SESSION_FILE : getUserSessionFile(sessionId);
        return SerializeUtils.readMapFromFile(sessionPath.toFile());
    }

    private synchronized void saveSessionData(String sessionId, Map<String, Object> sessionData) {
        var sessionPath = sessionId == null ? GLOBAL_SESSION_FILE : getUserSessionFile(sessionId);
        SerializeUtils.writeToFile(sessionPath.toFile(), sessionData);
    }

    public void cleanUp() {
        try {
            SerializeUtils.deleteDirectoryRecursively(this.SESSION_DIR);
            log.info("File manager directory deleted");
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file session folder", e);
        }
    }
}
