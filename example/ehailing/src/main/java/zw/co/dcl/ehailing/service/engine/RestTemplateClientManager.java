package zw.co.dcl.ehailing.service.engine;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.exceptions.ResponseException;
import zw.co.dcl.jawce.engine.api.exceptions.WhatsAppException;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.model.core.HookRest;

@Service
@Slf4j
public class RestTemplateClientManager implements IClientManager {
    private final RestTemplate client;

    public RestTemplateClientManager(RestTemplate client) {
        this.client = client;
    }

    @Override
    public <T> ResponseEntity<T> request(String url, HttpEntity<?> payload, HttpMethod action, Class<T> response) {
        var result = this.client.exchange(url, action, payload, response);
        log.debug("Request response code: {}", result.getStatusCode());
        return result;
    }

    @Override
    public ResponseEntity<String> post(String url, HookRest arg, HttpHeaders headers) {
        try {
            var response = this.request(url, new HttpEntity<>(arg, headers), HttpMethod.POST, String.class);
            log.debug("HookRest response: {}", response.getStatusCode());

            return response;
        } catch (Exception e) {
            log.error("HookRest call exception: {} | msg: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new InternalException("failed to process HookRest request", e);
        }
    }

    @Override
    public ResponseEntity<String> post(String url, Object payload, HttpHeaders headers) {
        try {
            var response = this.request(url, new HttpEntity<>(payload, headers), HttpMethod.POST, String.class);
            log.debug("POST request response: {}", response.getStatusCode());
            return response;
        } catch (HttpClientErrorException e) {
            log.error("Request exception: {}", e.getMessage());

            if(e.getStatusCode().equals(HttpStatus.UNAUTHORIZED)) {
                log.error("POST auth error: {}", e.getResponseBodyAsString());
                throw new WhatsAppException("Unauthorized access to WhatsApp. Check credentials");
            }

            if(e.getStatusCode().equals(HttpStatus.BAD_REQUEST)) {
                log.error("POST bad request: {}", e.getResponseBodyAsString());
                throw new WhatsAppException("Bad request to WhatsApp. Check request payload");
            }

            throw new InternalException("Failed to process WhatsApp request", e);
        } catch (ResponseException e) {
            throw e;
        } catch (Exception err) {
            log.error("WhatsApp request exception: {} | msg: {}", err.getClass().getSimpleName(), err.getMessage());
            throw new InternalException("Error sending WhatsApp request", err);
        } finally {
            MDC.remove(EngineConstant.MDC_WA_ID_KEY);
            MDC.remove(EngineConstant.MDC_WA_NAME_KEY);
        }
    }
}
