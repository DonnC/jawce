package zw.co.dcl.engine.whatsapp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.stereotype.Service;
import zw.co.dcl.engine.whatsapp.constants.SessionConstants;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

@Service
@Slf4j
public class SessionManager implements ISessionManager {
    private final CacheManager cacheManager;
    private Cache cache;

    public SessionManager(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    private void getCreateUserCache(String userId) {
        Cache userCache = cacheManager.getCache(userId);

        if (userCache == null) userCache = ((ConcurrentMapCacheManager) cacheManager).getCache(userId);
        assert userCache != null;
        this.cache = userCache;
    }

    @Override
    public SessionManager session(String user) {
        this.getCreateUserCache(user);
        return this;
    }

    public void save(String key, Object data) {
        this.cache.put(key, data);
    }

    @Override
    public boolean saveProp(String propKey, Object data) {
        try {
            Map<String, Object> props = (Map) this.get(SessionConstants.PROPS_KEY);
            if (props == null) {
                props = new HashMap<>();
            }
            props.put(propKey, data);
            this.save(SessionConstants.PROPS_KEY, props);
            return true;
        } catch (Exception e) {
            log.error("Failed to save prop to session: {}", e.getMessage());
        }
        return false;
    }

    public Object get(String key) {
        Cache.ValueWrapper valueWrapper = this.cache.get(key);
        return valueWrapper != null ? valueWrapper.get() : null;
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        return this.cache.get(key, type);
    }

    public ConcurrentMap<Object, Object> fetchAll() {
        if (cache != null) {
            return ((ConcurrentMapCache) cache).getNativeCache();
        }
        throw new EngineInternalException("Cache not found or not a ConcurrentMapCache");
    }

    public void evict(String key) {
        this.cache.evict(key);
    }

    public void clear() {
        System.out.println("user session cleared!");
        this.cache.clear();
    }
}
