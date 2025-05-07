package zw.co.dcl.jawce.engine.api;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.WhatsappUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.internal.events.OnceOffHookEvent;
import zw.co.dcl.jawce.engine.internal.events.WebhookEvent;
import zw.co.dcl.jawce.engine.internal.service.ClientHelperService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.core.WaUser;

import java.util.*;

@Slf4j
public class Worker {
    final ApplicationEventPublisher eventPublisher;
    final WhatsAppConfig waConfig;
    final JawceConfig jawceConfig;
    final ClientHelperService service;
    final WebhookProcessor webhookProcessor;
    ISessionManager session;

    public Worker(
            ApplicationEventPublisher eventPublisher,
            WhatsAppConfig whatsAppConfig,
            JawceConfig jawceConfig,
            ClientHelperService clientHelperService,
            WebhookProcessor webhookProcessor,
            ISessionManager sessionManager) {
        this.eventPublisher = eventPublisher;
        this.waConfig = whatsAppConfig;
        this.jawceConfig = jawceConfig;
        this.service = clientHelperService;
        this.webhookProcessor = webhookProcessor;
        this.session = sessionManager;
    }

    Set<String> getMessageQueue(WaUser user) {
        Set<String> queue = new HashSet<>();
        if(this.session.get(user.waId(), SessionConstant.SESSION_MESSAGE_HISTORY_KEY) != null) {
            var qHistory = this.session.get(user.waId(), SessionConstant.SESSION_MESSAGE_HISTORY_KEY, Set.class);
            queue = new HashSet<String>(qHistory);
        }
        return queue;
    }

    void addToMessageQueue(WaUser user) {
        Set<String> queue = getMessageQueue(user);
        queue.add(user.msgId());
        if(queue.size() > EngineConstant.MESSAGE_QUEUE_COUNT) {
            log.warn("Message queue limit reached, applying FIFO..");
            Iterator<String> iterator = queue.iterator();
            int count = 0;
            while (iterator.hasNext() && count < queue.size() - 10) {
                iterator.next();
                iterator.remove();
                count++;
            }
        }
        this.session.save(user.waId(), SessionConstant.SESSION_MESSAGE_HISTORY_KEY, queue);
    }

    void fireGlobalHook(String sessionId) {
        if(this.jawceConfig.getOnWebhookPrechecksComplete() != null) {
            var tempHook = new Hook();
            tempHook.setHook(this.jawceConfig.getOnWebhookPrechecksComplete());
            tempHook.setSessionId(sessionId);
            tempHook.setSession(this.session);

            eventPublisher.publishEvent(new OnceOffHookEvent(this, tempHook));
        }
    }

    Optional<Webhook> initChecks(Map<String, Object> webhookPayload, Map<String, Object> webhookHeaders) {
        var userOpt = WhatsappUtils.getUser(webhookPayload, webhookHeaders, this.jawceConfig.getWebhookTimestampThresholdSecs());
        if(userOpt.isEmpty()) return Optional.empty();

        var user = userOpt.get();
        var sessionId = user.waId();
        var message = WhatsappUtils.extractMessage(webhookPayload);
        var messageType = WhatsappUtils.isValidSupportedMessageType(message);
        this.session = this.session.session(sessionId);

        if(this.jawceConfig.isHandleSessionQueue()) {
            if(this.getMessageQueue(user).contains(user.msgId())) {
                log.warn("Duplicate message found: {}. Skipping..", message);
                return Optional.empty();
            }
        }

        Long lastDebounceTimestamp = this.session.get(sessionId, SessionConstant.CURRENT_DEBOUNCE_KEY, Long.class);
        long currentTime = System.currentTimeMillis();

        if(lastDebounceTimestamp == null || currentTime - lastDebounceTimestamp >= this.jawceConfig.getDebounceTimeoutMs()) {
            this.session.save(sessionId, SessionConstant.CURRENT_DEBOUNCE_KEY, currentTime);
        } else {
            log.warn("Message ignored due to debounce..");
            return Optional.empty();
        }

        if(this.jawceConfig.isHandleSessionQueue()) {
            this.addToMessageQueue(user);
        }

        return Optional.of(new Webhook(user, messageType, message));
    }

    public int verifyHubToken(String mode, String challenge, String token) {
        if("subscribe".equals(mode) && token.equals(this.waConfig.getHubToken())) return Integer.parseInt(challenge);
        throw new RuntimeException("Challenge failed, invalid hub token!");
    }

    public void processWebhook(Map<String, Object> webhookPayload, Map<String, Object> webhookHeaders) {
        var webhook = this.initChecks(webhookPayload, webhookHeaders);
        if(webhook.isEmpty()) return;
        this.fireGlobalHook(webhook.get().user().waId());

        try {
            var result = this.webhookProcessor.process(webhook.get());

            this.service.sendWhatsAppRequest(result);

            session.save(webhook.get().user().waId(), SessionConstant.CURRENT_MSG_ID_KEY, "user.msgId()");
        } catch (Exception e) {
            // TODO: handle all exceptions here
            log.error("Failed to process request", e);
        } finally {
            MDC.remove(EngineConstant.MDC_WA_ID_KEY);
            MDC.remove(EngineConstant.MDC_WA_NAME_KEY);
        }
    }

    @EventListener
    public void handleWebhookEvent(WebhookEvent event) {
        this.processWebhook(event.getPayload(), event.getHeaders());
    }
}
