package org.dcl.jawce.server.service;

import lombok.extern.slf4j.Slf4j;
import org.dcl.jawce.server.OnWhatsAppMessageEvent;
import org.dcl.jawce.server.constant.Constant;
import org.dcl.jawce.server.model.LiveModeCache;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.api.utils.WhatsAppUtils;
import zw.co.dcl.jawce.engine.internal.events.WebhookEvent;
import zw.co.dcl.jawce.engine.model.core.WaUser;

import java.util.Map;

@Slf4j
@Service
public class LsService {
    private final ApplicationEventPublisher eventPublisher;
    private final ISessionManager sessionManager;
    private final Worker jawceWorker;

    public LsService(ApplicationEventPublisher eventPublisher, ISessionManager sessionManager, Worker jawceWorker) {
        this.eventPublisher = eventPublisher;
        this.sessionManager = sessionManager;
        this.jawceWorker = jawceWorker;
    }

    private Long hasActiveLiveModeChatId(WaUser waUser) {
        String sessionId = waUser.waId();
        String cachedLsFlag = this.sessionManager
                .session(sessionId)
                .get(sessionId, Constant.LIVE_MODE_CACHE_KEY, String.class);

        if(cachedLsFlag == null) return null;

        LiveModeCache liveModeCache = SerializeUtils.castValue(cachedLsFlag, LiveModeCache.class);

        return liveModeCache.getActive() ? liveModeCache.getChatId() : null;
    }

    public int verifyToken(String mode, String token, String challenge) {
        return jawceWorker.verifyHubToken(mode, challenge, token);
    }

    /**
     * First check if user live mode session is active
     * If active, delegate processing to the live mode chat handler
     * else, let the automated bot handle it.
     * <p>
     * This approach can be used for almost anything
     * <p>
     * 1. Live Support
     * <p>
     * 2. AI Agent
     * <p>
     * 3. Anything along those lines
     *
     * @param payload WhatsApp webhook payload
     * @return String Acknowledge webhook
     */
    public String processRequest(Map<String, Object> payload) {
        try {
            WhatsAppUtils.getUser(payload).ifPresent(
                    (waUser) -> {
                        var chatId = hasActiveLiveModeChatId(waUser);
                        if(chatId != null) {
                            var waResponse = WhatsAppUtils.getResponseStructure(payload, true);
                            eventPublisher.publishEvent(new OnWhatsAppMessageEvent(this, waUser, waResponse, chatId));
                        } else {
                            eventPublisher.publishEvent(new WebhookEvent(this, payload));
                        }
                    }
            );

            return "OK!";
        } catch (Exception e) {
            log.error("Error processing webhook request", e);
            throw new RuntimeException(e.getMessage());
        }
    }
}
