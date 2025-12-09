package zw.co.dcl.jchatbot.service.engine;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.exceptions.ResponseException;
import zw.co.dcl.jawce.engine.api.exceptions.WhatsAppException;
import zw.co.dcl.jawce.engine.model.core.HookRest;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;

import java.util.Map;

@Service
@Slf4j
public class RestTemplateClientManager implements IClientManager {
    private final RestTemplate client;

    public RestTemplateClientManager(RestTemplate client) {
        this.client = client;
    }

    @Override
    public ResponseEntity<String> post(String url, HookRest arg, HttpHeaders headers) throws Exception {
        try {
            var response = this.client.postForEntity(url, new HttpEntity<>(arg, headers), String.class);
            log.debug("HookRest response: {}", response.getStatusCode());

            return response;
        } catch (Exception e) {
            log.error("HookRest call exception: {} | msg: {}", e.getClass().getSimpleName(), e.getMessage());
            throw new InternalException("failed to process HookRest request", e);
        }
    }

    @Override
    public ResponseEntity<String> post(String url, Map<String, Object> payload, HttpHeaders headers) throws Exception {
        try {
            var response = this.client.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
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
