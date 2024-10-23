package zw.co.dcl.jawce.engine.model.dto;

import org.springframework.web.client.RestTemplate;
import zw.co.dcl.jawce.engine.model.SessionSettings;
import zw.co.dcl.jawce.engine.model.WhatsappSettings;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public record WaEngineConfig(
        ISessionManager sessionManager,
        Map<String, Object> templateContext,
        Map<String, Object> triggerContext,
        EngineRequestSettings requestSettings,
        WhatsappSettings settings,
        SessionSettings sessionSettings,
        RestTemplate client
) {
}
