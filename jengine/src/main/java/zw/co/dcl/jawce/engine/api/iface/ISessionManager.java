package zw.co.dcl.jawce.engine.api.iface;

import java.util.List;
import java.util.Map;


/**
 * SessionManager handles all user session logic
 * <p>
 * The *Global methods are used in case an implementation has a single store
 * where some data needs to be cached globally for all users
 * <p>
 * The rest of the methods only caches a single user data
 * <p>
 * Props - these are user defined `additional-data` sort of.
 */
public interface ISessionManager {
    /**
     * @param sessionId unique session user id. Initiate a user session cache
     * @return ISessionManager instance
     */
    ISessionManager session(String sessionId);

    void save(String sessionId, String key, Object data);

    Object get(String sessionId, String key);

    Map<String, Object> fetchAll(String sessionId);

    void evict(String sessionId, String key);

    /**
     * Clear user session (excluding global data)
     */
    void clear(String sessionId);

    // --- helper methods
    void saveAll(String sessionId, Map<String, Object> sessionData);

    void evictAll(String sessionId, List<String> keys);

    void evictGlobal(String key);

    void clear(String sessionId, List<String> retain);

    boolean evictProp(String sessionId, String propKey);

    Object getFromProps(String sessionId, String propKey);

    boolean keyInSession(String sessionId, String key, boolean global);

    Map<String, Object> getUserProps(String sessionId);

    void saveGlobal(String key, Object data);

    void saveProp(String sessionId, String key, Object data);

    // ---  utility methods
    <T> T get(String sessionId, String key, Class<T> type);

    <T> T getGlobal(String key, Class<T> type);

    <T> T getFromProps(String sessionId, String propKey, Class<T> propType);
}
