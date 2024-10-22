package zw.co.dcl.jchatbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import zw.co.dcl.jchatbot.Util;
import zw.co.dcl.jchatbot.WebhookEvent;

@Service
public class WebhookEventProcessor {
    @Autowired
    private WebhookConfigService config;

    @Async
    @EventListener
    public void handleWebhook(WebhookEvent event) {
        config
                .getEntryInstance()
                .processWebhook(
                        event.getPayload(),
                        Util.requestHeadersToMap(event.getRequest())
                );
    }
}
