package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.model.SessionSettings;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public record MsgProcessorDTO(
        WaCurrentUser waCurrentUser,
        SupportedMessageType messageType,
        Map<String, Object> message
) {
}
