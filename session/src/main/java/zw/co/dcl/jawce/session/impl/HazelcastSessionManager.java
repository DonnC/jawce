package zw.co.dcl.jawce.session.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.session.Constants;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.session.utils.SessionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static zw.co.dcl.jawce.session.Constants.GLOBAL_SESSION_CACHE_NAME;
import static zw.co.dcl.jawce.session.Constants.USER_SESSION_CACHE_NAME;

/**
 * ISessionManager based on Hazelcast
 * <p>
 * The global session is configured to a default of 2 hours
 * <p>
 * Uses Hazelcast's IMap for session management with TTL.
 * <p>
 * You can use GLOBAL_SESSION_CACHE_NAME & USER_SESSION_CACHE_NAME for configuring MapConfig
 */
public class HazelcastSessionManager implements ISessionManager {
    private static final Logger LOGGER = Logger.getLogger(HazelcastSessionManager.class.getName());

    private static volatile HazelcastSessionManager instance;

    private final HazelcastInstance hazelcastInstance;
    private final IMap<String, Map<String, Object>> userSessionCache;
    private final IMap<String, Object> globalSessionCache;

    private HazelcastSessionManager(HazelcastInstance hazelInstance) {
        this.hazelcastInstance = hazelInstance;
        this.userSessionCache = createUserCache();
        this.globalSessionCache = createGlobalCache();
        LOGGER.info("Hazelcast session manager initialized");
    }

    public static HazelcastSessionManager getInstance(HazelcastInstance hazelInstance) {
        if(instance == null) {
            synchronized (HazelcastSessionManager.class) {
                if(instance == null) {
                    instance = new HazelcastSessionManager(hazelInstance);
                }
            }
        }

        return instance;
    }

    private IMap<String, Map<String, Object>> createUserCache() {
        return hazelcastInstance.getMap(USER_SESSION_CACHE_NAME);
    }

    private IMap<String, Object> createGlobalCache() {
        return hazelcastInstance.getMap(GLOBAL_SESSION_CACHE_NAME);
    }

    @Override
    public ISessionManager session(String user) {
        Assert.notNull(instance, "Failed to create HazelcastSessionManager instance");
        if(user == null) return this;

        userSessionCache.computeIfAbsent(user, k -> new HashMap<>());
        return this;
    }

    @Override
    public void save(String user, String key, Object data) {
        if(user == null) {
            globalSessionCache.put(key, data);
        } else {
            userSessionCache.computeIfAbsent(user, k -> new HashMap<>()).put(key, data);
        }
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
            var props = this.getUserProps(user);

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
        return this.getUserProps(user).get(propKey);
    }

    @Override
    public <T> T getFromProps(String user, String propKey, Class<T> propType) {
        Object propValue = getFromProps(user, propKey);
        return (propValue != null) ? propType.cast(propValue) : null;
    }

    @Override
    public Object get(String user, String key) {
        if(user == null) {
            return globalSessionCache.get(key);
        } else {
            Map<String, Object> sessionData = userSessionCache.get(user);
            return (sessionData != null) ? sessionData.get(key) : null;
        }
    }

    @Override
    public <T> T get(String user, String key, Class<T> type) {
        var data = this.get(user, key);
        return data == null ? null : SessionUtils.castValue(data, type);
    }

    @Override
    public <T> T getGlobal(String key, Class<T> type) {
        return this.get(null, key, type);
    }

    @Override
    public Map<String, Object> fetchAll(String user) {
        var data = userSessionCache.get(user);
        return Objects.requireNonNullElseGet(data, HashMap::new);
    }

    @Override
    public void evict(String user, String key) {
        Map<String, Object> sessionData = userSessionCache.get(user);

        if(sessionData != null && sessionData.containsKey(key)) {
            sessionData.remove(key);
            this.save(user, key, sessionData);
        }
    }

    @Override
    public void evictGlobal(String key) {
        globalSessionCache.delete(key);
    }

    @Override
    public void clear(String user) {
        userSessionCache.delete(user);
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
    public boolean keyInSession(String user, String key, boolean global) {
        if(global) {
            return globalSessionCache.containsKey(key);
        }

        Map<String, Object> sessionData = userSessionCache.get(user);
        return sessionData != null && sessionData.containsKey(key);
    }

    @Override
    public void clear(String user, List<String> retain) {
        Map<String, Object> sessionData = userSessionCache.get(user);
        if(sessionData != null) {
            sessionData.entrySet().removeIf(entry -> !retain.contains(entry.getKey()));
            userSessionCache.put(user, sessionData);
        }
    }

    @Override
    public Map<String, Object> getUserProps(String user) {
        var props = this.get(user, Constants.USER_PROPS_KEY, Map.class);
        return new HashMap<>(Objects.requireNonNullElseGet(props, HashMap::new));
    }

    public void deleteSessions() {
        userSessionCache.clear();
        globalSessionCache.clear();

        LOGGER.info("Hazelcast manager sessions deleted");
    }
}
