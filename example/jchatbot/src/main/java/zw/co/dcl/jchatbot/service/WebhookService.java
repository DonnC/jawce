package zw.co.dcl.jchatbot.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import zw.co.dcl.jchatbot.Util;
import zw.co.dcl.jchatbot.WebhookEvent;

import java.util.Map;

@Service
public class WebhookService {
    @Autowired
    private WebhookConfigService serviceConfig;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public ResponseEntity<?> verifyToken(String mode, String token, String challenge) {
        if(serviceConfig.getEntryInstance().verifyHubToken(mode, challenge, token).equals(challenge)) {
            return ResponseEntity.ok(Integer.parseInt(challenge));
        }
        return ResponseEntity.badRequest().build();
    }

    public ResponseEntity<String> processRequest(Map<String, Object> payload, HttpServletRequest request) {
        var headers = Util.requestHeadersToMap(request);
        eventPublisher.publishEvent(new WebhookEvent(this, payload, headers));
        return ResponseEntity.ok("SUCCESS");
    }
}
