package zw.co.dcl.jawce.engine.api.enums;

public enum MessageTypeEnum {
    UNSUPPORTED,
    MEDIA,
    CONTACTS,
    IMAGE,
    DOCUMENT,
    VIDEO,
    AUDIO,
    REACTION,
    ORDER,
    STICKER,
    TEXT,
    UNKNOWN, // for currently unsupported msgs
    CTA_BUTTON,
    BUTTON,
    TEMPLATE,
    INTERACTIVE,
    LOCATION,


    // inner interactive types
    INTERACTIVE_LIST,
    INTERACTIVE_FLOW,
    INTERACTIVE_BUTTON
}
