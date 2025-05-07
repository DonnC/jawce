package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.api.enums.WebhookIntrMsgType;
import zw.co.dcl.jawce.engine.api.enums.WebhookResponseMessageType;

public record SupportedMessageType(
        boolean isSupported,
        WebhookResponseMessageType type,
        WebhookIntrMsgType interactiveType // can be null
) {
}
