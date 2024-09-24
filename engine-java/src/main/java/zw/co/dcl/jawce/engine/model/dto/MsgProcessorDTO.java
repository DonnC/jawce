package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.model.SessionSettings;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public record MsgProcessorDTO(
        Map<String, Object> tplContextMap,
        Map<String, Object> triggersContextMap,
        ISessionManager sessionManager,
        WaCurrentUser waCurrentUser,
        SupportedMessageType messageType,
        Map<String, Object> message,
        SessionSettings sessionSettings
) {
}
