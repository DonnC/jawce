package zw.co.dcl.jawce.engine.service.iface;

/**
 * Implement this for making requests
 * <p>
 * Can create a RestTemplate implementation
 */
public interface IClientManager {
    Object process(Object request) throws Exception;
}
