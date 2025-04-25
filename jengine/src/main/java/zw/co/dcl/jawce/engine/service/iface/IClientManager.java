package zw.co.dcl.jawce.engine.service.iface;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import zw.co.dcl.jawce.engine.model.core.HookArgRest;

import java.util.Map;

/**
 * Implement this for making requests
 * <p>
 * Can create a RestTemplate implementation
 */
public interface IClientManager {
    ResponseEntity<String> post(String url, HookArgRest arg, HttpHeaders headers) throws Exception;

    ResponseEntity<String> post(String url, Map<String, Object> payload, HttpHeaders headers) throws Exception;
}
