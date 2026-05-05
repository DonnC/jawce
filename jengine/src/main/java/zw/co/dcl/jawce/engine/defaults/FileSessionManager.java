package zw.co.dcl.jawce.engine.defaults;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.configs.FileSessionProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Basic file-backed session manager intended as a default/reference implementation.
 *
 * Applications can replace this by providing their own ISessionManager bean.
 */
@Slf4j
public class FileSessionManager implements ISessionManager {
    private static final String USER_PROPS_KEY = "jProps";
    private static final String SESSION_FILE_EXT = ".session";

    @Getter
    private final Path sessionDir;
    @Getter
    private final Path globalSessionFile;

    public FileSessionManager(FileSessionProperties properties) {
        try {
            this.sessionDir = Paths.get(properties.getDir()).toAbsolutePath().normalize();
            Files.createDirectories(this.sessionDir);
            this.globalSessionFile = this.sessionDir.resolve("global" + SESSION_FILE_EXT).toAbsolutePath().normalize();
            createFileIfNotExist(globalSessionFile.toFile());
            log.info("File session manager initialized at {}", this.sessionDir);
        } catch (Exception e) {
            throw new RuntimeException("Cannot create session directory: " + e.getMessage(), e);
        }
    }

    private Path getUserSessionFile(String sessionId) {
        return this.sessionDir.resolve(sessionId + SESSION_FILE_EXT).toAbsolutePath().normalize();
    }

    private void createFileIfNotExist(File file) {
        if (!file.exists()) {
            SerializeUtils.writeToFile(file, new HashMap<>());
        }
    }

    @Override
    public ISessionManager session(String sessionId) {
        if (sessionId == null) {
            return this;
        }

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

        if (props == null) {
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
            if (props != null && props.containsKey(propKey)) {
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
            return props != null ? props.get(propKey) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public <T> T getFromProps(String sessionId, String propKey, Class<T> propType) {
        Object propValue = getFromProps(sessionId, propKey);
        return propValue != null ? propType.cast(propValue) : null;
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
        if (retain.isEmpty()) {
            return;
        }

        List<String> keysToEvict = new ArrayList<>();

        fetchAll(sessionId).forEach((k, v) -> {
            if (!retain.contains(k)) {
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
        var sessionPath = sessionId == null ? globalSessionFile : getUserSessionFile(sessionId);
        return SerializeUtils.readMapFromFile(sessionPath.toFile());
    }

    private synchronized void saveSessionData(String sessionId, Map<String, Object> sessionData) {
        var sessionPath = sessionId == null ? globalSessionFile : getUserSessionFile(sessionId);
        SerializeUtils.writeToFile(sessionPath.toFile(), sessionData);
    }

    public void cleanUp() {
        try {
            SerializeUtils.deleteDirectoryRecursively(this.sessionDir);
            log.info("File session directory deleted");
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file session folder", e);
        }
    }
}
