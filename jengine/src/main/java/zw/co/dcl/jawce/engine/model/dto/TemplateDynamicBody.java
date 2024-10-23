package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;

import java.util.Map;

public record TemplateDynamicBody(
        WebhookResponseMessageType type,
        // for Flow payload
        Map<String, Object> payload,
        // for Template payload
        Map<String, Object> renderPayload
) {
}
