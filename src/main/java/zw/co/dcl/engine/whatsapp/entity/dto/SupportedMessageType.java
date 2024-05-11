package zw.co.dcl.engine.whatsapp.entity.dto;

import zw.co.dcl.engine.whatsapp.enums.WebhookIntrMsgType;
import zw.co.dcl.engine.whatsapp.enums.WebhookResponseMessageType;

public record SupportedMessageType(
        boolean isSupported,
        WebhookResponseMessageType type,
        WebhookIntrMsgType intrType // can be null
) {
}
