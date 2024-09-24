package zw.co.dcl.jawce.engine.enums;

public enum WebhookResponseMessageType {
    UNSUPPORTED,
    MEDIA,
    IMAGE,
    DOCUMENT,
    TEXT,
    UNKNOWN, // for currently unsupported msgs
    BUTTON,
    TEMPLATE,
    INTERACTIVE,
    LOCATION
}