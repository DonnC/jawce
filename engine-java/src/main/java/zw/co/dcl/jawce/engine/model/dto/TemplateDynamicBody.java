package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;

import java.util.Map;

public record TemplateDynamicBody(
        WebhookResponseMessageType type,
        Map<String, Object> payload,

        /**
         * if defined flow template has dynamic variable fields
         * <p>
         *     set them here to be dynamically rendered
         */
        Map<String, Object> renderPayload
) {
}
