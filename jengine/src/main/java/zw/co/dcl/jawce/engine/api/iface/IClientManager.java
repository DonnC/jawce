package zw.co.dcl.jawce.engine.api.iface;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import zw.co.dcl.jawce.engine.model.core.HookRest;

import java.util.Map;

/**
 * Implement this for making requests
 * <p>
 * Can create a RestTemplate implementation
 */
public interface IClientManager {
    // for rest based hooks
    ResponseEntity<String> post(String url, HookRest arg, HttpHeaders headers) throws Exception;

    // send whatsapp message
    ResponseEntity<String> post(String url, Object payload, HttpHeaders headers) throws Exception;

    // send generic request
    <T> ResponseEntity<T> request(String url, HttpEntity<?> payload, HttpMethod action, Class<T> response) throws Exception;
}
