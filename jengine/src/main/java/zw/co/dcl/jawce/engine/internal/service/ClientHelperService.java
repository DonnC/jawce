package zw.co.dcl.jawce.engine.internal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.api.utils.Utils;
import zw.co.dcl.jawce.engine.api.utils.WhatsappUtils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.model.dto.WebhookProcessorResult;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;

@Slf4j
public class ClientHelperService {
    private final IClientManager clientManager;
    private final ISessionManager sessionManager;
    private final JawceConfig config;
    private final WhatsAppConfig whatsAppConfig;

    public ClientHelperService(
            IClientManager clientManager, ISessionManager sessionManager,
            JawceConfig config, WhatsAppConfig whatsAppConfig
    ) {
        this.clientManager = clientManager;
        this.sessionManager = sessionManager;
        this.config = config;
        this.whatsAppConfig = whatsAppConfig;
    }

    void onWhatsappRequestSuccess(WebhookProcessorResult requestDto) {
        var session = this.sessionManager.session(requestDto.sessionId());
        if(requestDto.handleSession()) {
            session.evict(requestDto.sessionId(), SessionConstant.CURRENT_STAGE_RETRY_COUNT);
            var stageCode = session.get(requestDto.sessionId(), SessionConstant.CURRENT_STAGE);
            session.save(requestDto.sessionId(), SessionConstant.PREV_STAGE, stageCode);
            session.save(requestDto.sessionId(), SessionConstant.CURRENT_STAGE, requestDto.nextRoute());
            log.debug("[onSuccess] Current route set to: {}", requestDto.nextRoute());
        }
        if(config.isHandleSessionInactivity()) {
            session.save(
                    requestDto.sessionId(),
                    SessionConstant.LAST_ACTIVITY_KEY,
                    Utils.formatZonedDateTime(Utils.currentSystemDate())
            );
        }
    }

    void onRequestError(String sessionId) {
        var session = this.sessionManager.session(sessionId);
        if(session.get(sessionId, SessionConstant.PREV_STAGE)
                .toString()
                .equalsIgnoreCase(config.getStartMenu()) ||
                session.get(sessionId, SessionConstant.CURRENT_STAGE)
                        .toString()
                        .equalsIgnoreCase(config.getStartMenu())
        ) {
            log.warn("WhatsApp request exception - clearing session");
            session.clear(sessionId);
        } else {
            session.save(sessionId, SessionConstant.CURRENT_STAGE, session.get(sessionId, SessionConstant.PREV_STAGE));
        }
    }

    public String sendWhatsAppRequest(WebhookProcessorResult requestDto) throws Exception {
        try {
            var response = this.clientManager.post(
                    WhatsappUtils.getUrl(this.whatsAppConfig),
                    requestDto.payload(),
                    WhatsappUtils.getHeaders(this.whatsAppConfig)
            );

            if(WhatsappUtils.isValidRequestResponse(response.getBody())) {
                this.onWhatsappRequestSuccess(requestDto);
                return response.getBody();
            }

            log.error("WhatsApp invalid response. Code: {} | Body: {}", response.getStatusCode(), response.getBody());
            throw new InternalException("There was a problem. Unsuccessful channel response code");
        } catch (Exception e) {
            this.onRequestError(requestDto.sessionId());
            throw new InternalException("Failed to process Whatsapp Cloud request", e);
        }
    }
}
