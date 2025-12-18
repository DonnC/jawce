package zw.co.dcl.ehailing.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.engine.internal.events.WebhookEvent;

import java.util.Map;

@Service
public class WebhookService {
    private final ApplicationEventPublisher eventPublisher;
    private final Worker jawceWorker;

    public WebhookService(ApplicationEventPublisher eventPublisher, Worker jawceWorker) {
        this.eventPublisher = eventPublisher;
        this.jawceWorker = jawceWorker;
    }

    public int verifyToken(String mode, String token, String challenge) {
        return jawceWorker.verifyHubToken(mode, challenge, token);
    }

    public String processRequest(Map<String, Object> payload) {
        eventPublisher.publishEvent(new WebhookEvent(this, payload));
        return "ACK!";
    }
}
