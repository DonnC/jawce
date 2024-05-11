package zw.co.dcl.engine.whatsapp.entity.dto;

import zw.co.dcl.engine.whatsapp.entity.SessionSettings;
import zw.co.dcl.engine.whatsapp.entity.WhatsappSettings;
import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;

import java.util.Map;

public record WaEngineConfig(
        ISessionManager ISessionManager,
        Map<String, Object> templateContext,
        Map<String, Object> triggerContext,
        EngineHookSettings requestSettings,
        WhatsappSettings settings,
        SessionSettings sessionSettings
) {
}
