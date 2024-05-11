package zw.co.dcl.engine.whatsapp.entity.dto;

import zw.co.dcl.engine.whatsapp.enums.WebhookResponseMessageType;

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
