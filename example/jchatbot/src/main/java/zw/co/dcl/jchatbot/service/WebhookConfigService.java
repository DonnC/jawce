package zw.co.dcl.jchatbot.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.model.SessionSettings;
import zw.co.dcl.jawce.engine.model.WhatsappSettings;
import zw.co.dcl.jawce.engine.model.dto.ChannelOriginConfig;
import zw.co.dcl.jawce.engine.model.dto.EngineRequestSettings;
import zw.co.dcl.jawce.engine.model.dto.WaEngineConfig;
import zw.co.dcl.jawce.engine.service.EntryService;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jchatbot.configs.ChatbotConfig;
import zw.co.dcl.jchatbot.configs.TemplateConfig;

import java.util.ArrayList;
import java.util.Map;

@Service
public class WebhookConfigService {

    private final ISessionManager sessionManager;
    private final ChatbotConfig config;
    private final TemplateConfig templateConfig;
    private final RestTemplate restTemplate;

    private final Map<String, Object> templatesContextMap;
    private final Map<String, Object> triggersContextMap;

    public WebhookConfigService(
            @Qualifier("botTemplates") Map<String, Object> templatesMap,
            @Qualifier("botTriggers") Map<String, Object> triggersMap,
            ISessionManager sessionManager,
            ChatbotConfig config,
            TemplateConfig templateConfig,
            RestTemplate restTemplate
    ) {
        this.templatesContextMap = (Map) templatesMap.get("botTemplates");
        this.triggersContextMap = (Map) triggersMap.get("botTriggers");
        this.sessionManager = sessionManager;
        this.config = config;
        this.templateConfig = templateConfig;
        this.restTemplate = restTemplate;
    }

    public EntryService getEntryInstance() {
        var channelOrigin = new ChannelOriginConfig(
                false,
                new ArrayList<>(),
                false,
                null,
                "*"
        );

        return EntryService.getInstance(engineConfig(), channelOrigin);
    }

    private WaEngineConfig engineConfig() {
        return new WaEngineConfig(
                sessionManager,
                templatesContextMap,
                triggersContextMap,
                new EngineRequestSettings(
                        templateConfig.getBotEngineHookBaseUrl(),
                        templateConfig.getBotEngineHookUrlToken()
                ),
                whatsappChannelSettings(),
                sessionSettings(),
                restTemplate
        );
    }

    private WhatsappSettings whatsappChannelSettings() {
        var settings = new WhatsappSettings();

        settings.setHubToken(config.getHubToken());
        settings.setAccessToken(config.getAccessToken());
        settings.setPhoneNumberId(config.getPhoneNumberId());
        settings.setApiVersion("v21.0");

        return settings;
    }

    private SessionSettings sessionSettings() {
        var settings = new SessionSettings();
        settings.setSessionTTL(config.getSessionTtl());
        settings.setHandleSessionInactivity(false);
        settings.setStartMenuStageKey(config.getInitialStage());
        return settings;
    }

    public void clearSession(String userSessionId) {
        sessionManager.clear(userSessionId);
    }
}
