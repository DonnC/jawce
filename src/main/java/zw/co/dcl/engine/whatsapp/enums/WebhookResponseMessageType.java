package zw.co.dcl.engine.whatsapp.enums;

public enum WebhookResponseMessageType {
    UNSUPPORTED,
    IMAGE,
    DOCUMENT,
    TEXT,
    UNKNOWN, // for currently unsupported msgs
    BUTTON,
    TEMPLATE,
    INTERACTIVE
}
