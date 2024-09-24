package zw.co.dcl.jawce.session.impl;

import org.springframework.data.redis.core.RedisTemplate;
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
 * ISessionManager implementation based on Redis.
 * <p>
 * The upstream client can provide their own Redis configuration via Spring's application properties or YAML files.
 */
public class RedisSessionManager implements ISessionManager {
    private final static Logger LOGGER = Logger.getLogger(RedisSessionManager.class.getName());

    private static volatile RedisSessionManager instance;

    private final RedisTemplate<String, Object> redisTemplate;
    private final long sessionTimeout;
    private final TimeUnit timeUnit;

    private RedisSessionManager(
            RedisTemplate<String, Object> redisTemplate,
            long sessionTimeout,
            TimeUnit timeUnit
    ) {
        this.redisTemplate = redisTemplate;
        this.sessionTimeout = sessionTimeout;
        this.timeUnit = timeUnit;
        LOGGER.info("Redis session manager initialized");
    }

    public static RedisSessionManager getInstance(
            RedisTemplate<String, Object> redisTemplate,
            long sessionTimeout,
            TimeUnit timeUnit
    ) {
        if(instance == null) {
            synchronized (RedisSessionManager.class) {
                if(instance == null) {
                    instance = new RedisSessionManager(redisTemplate, sessionTimeout, timeUnit);
                }
            }
        }

        return instance;
    }

    @Override
    public ISessionManager session(String user) {
        Assert.notNull(instance, "Failed to create RedisSessionManager instance");
        if(user == null) return this;

        // Initialize an empty session for the user if not present
        if(redisTemplate.opsForHash().entries(user).isEmpty()) {
            redisTemplate.opsForHash().put(user, Constants.USER_PROPS_KEY, new HashMap<>());
            redisTemplate.expire(user, sessionTimeout, timeUnit);
        }

        return this;
    }

    @Override
    public void save(String user, String key, Object data) {
        if(user == null) {
            redisTemplate.opsForValue().set(key, data, sessionTimeout, timeUnit);
        } else {
            redisTemplate.opsForHash().put(user, key, data);
            redisTemplate.expire(user, sessionTimeout, timeUnit);
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
            return redisTemplate.opsForValue().get(key);
        } else {
            return redisTemplate.opsForHash().get(user, key);
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
        var data = redisTemplate.opsForHash().entries(user);
        return SessionUtils.convertMap(Objects.requireNonNullElseGet(data, HashMap::new));
    }

    @Override
    public void evict(String user, String key) {
        redisTemplate.opsForHash().delete(user, key);
    }

    @Override
    public void evictGlobal(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void clear(String user) {
        redisTemplate.delete(user);
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
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        }

        return redisTemplate.opsForHash().hasKey(user, key);
    }

    @Override
    public void clear(String user, List<String> retain) {
        Map<String, Object> sessionData = this.fetchAll(user);
        sessionData.entrySet().removeIf(entry -> !retain.contains(entry.getKey()));
        redisTemplate.opsForHash().putAll(user, sessionData);
    }

    @Override
    public Map<String, Object> getUserProps(String user) {
        var props = this.get(user, Constants.USER_PROPS_KEY, Map.class);
        return new HashMap<>(Objects.requireNonNullElseGet(props, HashMap::new));
    }

    public void deleteSessions() {
        // TODO Use Redis commands to delete all sessions
        // LOGGER.info("Redis manager sessions deleted");
    }
}
