package zw.co.dcl.jawce.engine.internal.dto;

import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.SupportedMessageType;

import java.util.Map;

public record Webhook(
        WaUser user,
        SupportedMessageType type,
        Map<String, Object> message
) {
}
