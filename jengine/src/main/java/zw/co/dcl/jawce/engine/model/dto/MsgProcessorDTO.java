package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.model.core.WaUser;

import java.util.Map;

public record MsgProcessorDTO(
        WaUser user,
        SupportedMessageType messageType,
        Map<String, Object> message
) {
}
