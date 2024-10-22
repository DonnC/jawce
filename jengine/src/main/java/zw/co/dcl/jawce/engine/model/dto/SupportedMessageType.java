package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.enums.WebhookIntrMsgType;
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;

public record SupportedMessageType(
        boolean isSupported,
        WebhookResponseMessageType type,
        WebhookIntrMsgType intrType // can be null
) {
}
