package zw.co.dcl.jawce.engine.api.enums;

/*
text: for text messages.
template: for template messages. Only text-based templates are supported.
document: for document messages.
image: for image messages.
interactive: for list and reply button messages.
audio: for audio messages.
contacts: for contacts messages.
location: for location messages.
sticker: for sticker messages.
video:

 */

public enum PayloadType {
    TEXT,
    INTERACTIVE,
    DOCUMENT,
    IMAGE,
    TEMPLATE,
    REACTION,
    LOCATION,
    MEDIA
}
