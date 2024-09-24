package zw.co.dcl.jawce.session;

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
     * @param user unique session user id. Initiate a user session cache
     * @return ISessionManager instance
     */
    ISessionManager session(String user);

    void save(String user, String key, Object data);

    Object get(String user, String key);

    Map<String, Object> fetchAll(String user);

    void evict(String user, String key);

    /**
     * Clear user session (excluding global data)
     */
    void clear(String user);

    // --- helper methods
    void saveAll(String user, Map<String, Object> sessionData);

    void evictAll(String user, List<String> keys);

    void evictGlobal(String key);

    void clear(String user, List<String> retain);

    boolean evictProp(String user, String propKey);

    Object getFromProps(String user, String propKey);

    boolean keyInSession(String user, String key, boolean global);

    Map<String, Object> getUserProps(String user);

    void saveGlobal(String key, Object data);

    void saveProp(String user, String key, Object data);

    // ---  utility methods
    <T> T get(String user, String key, Class<T> type);

    <T> T getGlobal(String key, Class<T> type);

    <T> T getFromProps(String user, String propKey, Class<T> propType);
}
