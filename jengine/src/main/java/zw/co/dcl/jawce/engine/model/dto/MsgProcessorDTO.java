package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.service.iface.ISessionManager;

import java.util.Map;

public record MsgProcessorDTO(
        WaUser user,
        SupportedMessageType messageType,
        Map<String, Object> message,
        ISessionManager session
) {
}
