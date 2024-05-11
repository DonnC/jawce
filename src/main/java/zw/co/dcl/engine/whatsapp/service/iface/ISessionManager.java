package zw.co.dcl.engine.whatsapp.service.iface;

import java.util.Map;

public interface ISessionManager {
    public ISessionManager session(String user);

    public void save(String key, Object data);

    public boolean saveProp(String key, Object data);


    public Object get(String key);

    public <T> T get(String key, Class<T> type);

    public void evict(String key);

    public void clear();
}
