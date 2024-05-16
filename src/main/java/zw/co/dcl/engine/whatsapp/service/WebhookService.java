package zw.co.dcl.engine.whatsapp.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import zw.co.dcl.engine.whatsapp.entity.ConfigEntity;
import zw.co.dcl.engine.whatsapp.entity.SessionSettings;
import zw.co.dcl.engine.whatsapp.entity.WhatsappSettings;
import zw.co.dcl.engine.whatsapp.entity.dto.ChannelOriginConfig;
import zw.co.dcl.engine.whatsapp.entity.dto.EngineHookSettings;
import zw.co.dcl.engine.whatsapp.entity.dto.RestSaveSession;
import zw.co.dcl.engine.whatsapp.entity.dto.WaEngineConfig;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.repository.ConfigEntityRepository;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class WebhookService {
    final ConfigEntityRepository configRepository;
    final SessionManager sessionManager;

    private final WhatsappEngineProcessor engineProcessor;

    public WebhookService(
            ConfigEntityRepository configRepository,
            SessionManager sessionManager,
            @Qualifier("templatesMapConfig") Map<String, Object> templatesContextMap,
            @Qualifier("triggersMapConfig") Map<String, Object> triggersContextMap

    ) {
        this.configRepository = configRepository;
        this.sessionManager = sessionManager;

        ConfigEntity channelConfig = getConfig();

        WhatsappSettings whatsappSettings = new WhatsappSettings();
        whatsappSettings.setAccessToken(channelConfig.getAccessToken());
        whatsappSettings.setApiVersion(channelConfig.getApiVersion());
        whatsappSettings.setHubToken(channelConfig.getHubToken());
        whatsappSettings.setPhoneNumberId(channelConfig.getPhoneNumberId());

        SessionSettings sessionSettings = new SessionSettings();
        sessionSettings.setStartMenuStageKey("MAIN_MENU");

        this.engineProcessor = new WhatsappEngineProcessor(
                new WaEngineConfig(
                        sessionManager,
                        (Map) templatesContextMap.get("templatesMapConfig"),
                        (Map) triggersContextMap.get("triggersMapConfig"),
                        new EngineHookSettings(null, null),
                        whatsappSettings,
                        sessionSettings
                ),
                new ChannelOriginConfig(
                        false,
                        List.of(Pattern.compile("^263")),
                        false,
                        null,
                        "*"

                )
        );
    }


    public ConfigEntity setConfig(ConfigEntity config) {
        log.info("CONFIG: {}", config);
        configRepository.deleteAll();
        return configRepository.save(config);
    }

    public ConfigEntity getConfig() {
        if (configRepository.count() != 0) {
            return configRepository.findAll().iterator().next();
        }

        log.error("No WhatsApp config found: [{}]. Consider adding configs via POST: /webhook/config", configRepository.count());

        return new ConfigEntity(
                0L,
                "accessToken",
                "hubToken",
                "phoneNumberId",
                "v18.0"
        );
    }

    public ResponseEntity<?> verifyToken(String mode, String challenge, String token, HttpServletRequest request) {
        log.info("verifying webhook token from: {}", CommonUtils.requestHeadersToMap(request));

        if (engineProcessor.verifyHubToken(mode, challenge, token).equals(challenge))
            return ResponseEntity.ok(Integer.parseInt(challenge));

        return ResponseEntity.badRequest().build();
    }

    public Map processWebhook(Object payload, HttpServletRequest request) {
        if (engineProcessor.verifyWebhookPayload(payload, request) != null)
            engineProcessor.processWebhook(payload, request);

        return Map.of("status", "ACK");
    }


    public Map getDataFromSession(String user, String key) {
        if (key == null) throw new EngineInternalException("key is null");

        if (key.equals("*")) return Map.of(
                "key", key,
                "data", sessionManager.session(user).fetchAll()
        );

        return Map.of(
                "key", key,
                "data", sessionManager.session(user).get(key) == null
                        ? "" :
                        sessionManager.session(user).get(key)
        );
    }

    public void saveToSession(RestSaveSession dto) {
        if (dto.prop()) {
            sessionManager.session(dto.user()).saveProp(dto.key(), dto.data());
        } else
            sessionManager.session(dto.user()).save(dto.key(), dto.data());
    }
}
