package zw.co.dcl.jawce.session.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.session.Constants;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jawce.session.utils.SessionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * ISessionManager based on Caffeine
 * <p>
 * The global session is configured to a default of 2hr
 * <p>
 * The builder method, uses the .expireAfterWrite method to set TTL
 * <p>
 * Eg. CaffeineSessionManager.getInstance().configureCache(5, TimeUnit.MINUTES);
 */
public class CaffeineSessionManager implements ISessionManager {
    private final static Logger LOGGER = Logger.getLogger(CaffeineSessionManager.class.getName());

    private static volatile CaffeineSessionManager instance;

    private final Cache<String, Map<String, Object>> userSessionCache;
    private final Cache<String, Object> globalSessionCache;

    private CaffeineSessionManager(long duration, TimeUnit timeUnit) {
        this.userSessionCache = createUserCache(duration, timeUnit);
        this.globalSessionCache = createGlobalCache();
        LOGGER.info("Caffeine session manager initialized");
    }

    public static CaffeineSessionManager getInstance(long duration, TimeUnit timeUnit) {
        if(instance == null) {
            synchronized (CaffeineSessionManager.class) {
                if(instance == null) {
                    instance = new CaffeineSessionManager(duration, timeUnit);
                }
            }
        }

        return instance;
    }

    private Cache<String, Map<String, Object>> createUserCache(long duration, TimeUnit timeUnit) {
        return Caffeine.newBuilder()
                .expireAfterWrite(duration, timeUnit)
                .build();
    }

    private Cache<String, Object> createGlobalCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(12, TimeUnit.HOURS)
                .build();
    }

    @Override
    public ISessionManager session(String user) {
        Assert.notNull(instance, "Failed to create CaffeineSessionManager instance");
        if(user == null) return this;

        if(userSessionCache.getIfPresent(user) == null) {
            userSessionCache.put(user, new HashMap<>());
        }

        return this;
    }

    @Override
    public void save(String user, String key, Object data) {
        if(user == null) {
            globalSessionCache.put(key, data);
        } else {
            userSessionCache.get(user, k -> new HashMap<>()).put(key, data);
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
            return globalSessionCache.getIfPresent(key);
        } else {
            Map<String, Object> sessionData = userSessionCache.getIfPresent(user);
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
        var data = userSessionCache.getIfPresent(user);
        return Objects.requireNonNullElseGet(data, HashMap::new);
    }

    @Override
    public void evict(String user, String key) {
        Map<String, Object> sessionData = userSessionCache.getIfPresent(user);

        if(sessionData != null && sessionData.containsKey(key)) {
            sessionData.remove(key);
            this.save(user, key, sessionData);
        }
    }

    @Override
    public void evictGlobal(String key) {
        globalSessionCache.invalidate(key);
    }

    @Override
    public void clear(String user) {
        userSessionCache.invalidate(user);
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
            return globalSessionCache.getIfPresent(key) != null;
        }

        Map<String, Object> sessionData = userSessionCache.getIfPresent(user);
        return sessionData != null && sessionData.containsKey(key);
    }

    @Override
    public void clear(String user, List<String> retain) {
        Map<String, Object> sessionData = userSessionCache.getIfPresent(user);
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
        userSessionCache.invalidateAll();
        globalSessionCache.invalidateAll();

        userSessionCache.cleanUp();
        globalSessionCache.cleanUp();

        LOGGER.info("Caffeine manager sessions deleted");
    }
}
