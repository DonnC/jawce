package zw.co.dcl.jawce.session.impl;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.lmdbjava.*;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.session.Constants;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.session.utils.SessionUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

import static zw.co.dcl.jawce.session.Constants.*;

public class LmdbSessionManager implements ISessionManager {
    private final static Logger LOGGER = Logger.getLogger(LmdbSessionManager.class.getName());

    private static volatile LmdbSessionManager instance;

    private final Env<DirectBuffer> env;
    private final Dbi<DirectBuffer> dbi;
    private final MutableDirectBuffer keyBuffer;
    private final MutableDirectBuffer valueBuffer;
    private final Path SESSION_DIR;

    private LmdbSessionManager(Long mapSize, Integer bufferCapacity) {
        try {
            this.SESSION_DIR = Paths.get(LMDB_FOLDER).toAbsolutePath().normalize();

            // Ensure the folder exists; if not, create it
            var folder = this.SESSION_DIR.toFile();
            if(!folder.exists()) {
                folder.mkdirs();
            }

            if(mapSize == null) {
                mapSize = 10_485_760L;
            }

            if(bufferCapacity == null) {
                bufferCapacity = 1024;
            }

            this.env = Env.create(DirectBufferProxy.PROXY_DB)
                    .setMapSize(mapSize)
                    .setMaxDbs(1)
                    .open(folder);

            this.dbi = env.openDbi(SESSION_DB_NAME, DbiFlags.MDB_CREATE);

            // Initialize the direct buffers for keys and values
            ByteBuffer keyByteBuffer = ByteBuffer.allocateDirect(env.getMaxKeySize());
            this.keyBuffer = new UnsafeBuffer(keyByteBuffer);
            this.valueBuffer = new UnsafeBuffer(ByteBuffer.allocateDirect(bufferCapacity));
            LOGGER.info("LMDB session manager initialized!");
        } catch (Exception e) {
            throw new RuntimeException("Cannot create LMDB environment: " + e.getMessage());
        }
    }

    /**
     * @param mapSize        max capacity to expand to.
     *                       defaults to 10MB
     * @param bufferCapacity maximum buffer to allocate to each value.
     *                       defaults to 1KB
     */
    public static LmdbSessionManager getInstance(Long mapSize, Integer bufferCapacity) {
        if(instance == null) {
            synchronized (LmdbSessionManager.class) {
                if(instance == null) {
                    instance = new LmdbSessionManager(mapSize, bufferCapacity);
                }
            }
        }
        return instance;
    }


    @Override
    public ISessionManager session(String user) {
        Assert.notNull(instance, "Failed to create LmdbSessionManager instance");
        return this;
    }

    @Override
    public void save(String user, String key, Object data) {
        var sessionData = loadSessionData(user);
        sessionData.put(key, data);
        saveSessionData(user, sessionData);
    }

    @Override
    public void saveGlobal(String key, Object data) {
        save(null, key, data);
    }

    @Override
    public void saveProp(String user, String propKey, Object data) {
        var props = this.getUserProps(user);
        props.put(propKey, data);
        this.save(user, Constants.USER_PROPS_KEY, props);
    }

    @Override
    public boolean evictProp(String user, String propKey) {
        try {
            var props = getUserProps(user);
            if(props.containsKey(propKey)) {
                props.remove(propKey);
                this.save(user, Constants.USER_PROPS_KEY, props);
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.severe("Error evicting prop: " + propKey + ", message: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Object getFromProps(String user, String propKey) {
        return getUserProps(user).get(propKey);
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
    public void evict(String user, String key) {
        var sessionData = loadSessionData(user);
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

    private Map<String, Object> loadSessionData(String user) {
        keyBuffer.putStringWithoutLengthUtf8(0, user == null ? GLOBAL_SESSION_KEY : user);

        try (Txn<DirectBuffer> txn = env.txnRead()) {
            DirectBuffer foundBuffer = dbi.get(txn, keyBuffer);
            if(foundBuffer == null) {
                return new HashMap<>();
            }
            byte[] data = new byte[foundBuffer.capacity()];
            foundBuffer.getBytes(0, data);

            return SessionUtils.byteToMap(data);
        }
    }

    private void saveSessionData(String user, Map<String, Object> sessionData) {
        keyBuffer.putStringWithoutLengthUtf8(0, user == null ? GLOBAL_SESSION_KEY : user);

        try (Txn<DirectBuffer> txn = env.txnWrite()) {
            try (Cursor<DirectBuffer> cursor = dbi.openCursor(txn)) {
                byte[] serializedData = SessionUtils.toBytes(sessionData);
                valueBuffer.putBytes(0, serializedData);
                cursor.put(keyBuffer, valueBuffer);
            }
            txn.commit();
        }
    }

    public void deleteSessionFolder() {
        try {
            if(dbi != null) {
                dbi.close();
            }
            if(env != null) {
                env.close();
            }

            SessionUtils.deleteDirectoryRecursively(this.SESSION_DIR);

            LOGGER.info("LMDB manager session directory deleted");
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete LMDB session folder", e);
        }
    }
}
