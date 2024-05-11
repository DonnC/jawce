package zw.co.dcl.engine.whatsapp.entity.dto;

import zw.co.dcl.engine.whatsapp.entity.SessionSettings;
import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;

import java.util.Map;

public record MsgProcessorDTO(
        Map<String, Object> tplContextMap,
        Map<String, Object> triggersContextMap,
        ISessionManager ISessionManager,
        WaCurrentUser waCurrentUser,
        SupportedMessageType messageType,
        Map<String, Object> message,
        SessionSettings sessionSettings
) {
}
