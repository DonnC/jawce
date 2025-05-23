package zw.co.dcl.jawce.engine.api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.engine.api.enums.ListSectionType;
import zw.co.dcl.jawce.engine.api.enums.PayloadType;
import zw.co.dcl.jawce.engine.api.enums.WebhookIntrMsgType;
import zw.co.dcl.jawce.engine.api.enums.WebhookResponseMessageType;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.exceptions.ResponseException;
import zw.co.dcl.jawce.engine.api.exceptions.WhatsappException;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.internal.dto.ResponseError;
import zw.co.dcl.jawce.engine.internal.dto.UserInput;
import zw.co.dcl.jawce.engine.internal.dto.Webhook;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.SupportedMessageType;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class WhatsappUtils {
    static final List<String> supportedTypes = List.of("text", "document", "interactive", "button",
            "unknown", "location", "image", "video", "audio", "sticker");

    public static Optional<WaUser> getUser(Map<String, Object> webhookPayload, int webhookTtl) {
        if(isRequestErrorMessage(webhookPayload)) {
            throw new WhatsappException(webhookPayload.toString());
        }
        if(!isValidWebhookMessage(webhookPayload)) {
            throw new InternalException("invalid channel message received");
        }

        if(hasChannelMsgObject(webhookPayload)) {
            var msgData = extractMessage(webhookPayload);

            var supportedMsgType = isValidSupportedMessageType(msgData);
            if(!supportedMsgType.isSupported()) throw new InternalException("unsupported message type");

            WaUser waUser = computeUser(webhookPayload);

            MDC.put(EngineConstant.MDC_WA_ID_KEY, waUser.waId());
            MDC.put(EngineConstant.MDC_WA_NAME_KEY, waUser.name());

            if(isOldWebhook(waUser.timestamp(), webhookTtl)) {
                log.warn("OLD WEBHOOK REQ RECEIVED: {}. DISCARDED", convertTimestamp(waUser.timestamp()));
                return Optional.empty();
            }

            return Optional.of(waUser);
        }

        log.warn("No message obj, ignoring..");
        return Optional.empty();
    }

    public static String getUrl(WhatsAppConfig config) {
        return config.getLocalUrl() != null ?
                config.getLocalUrl() :
                EngineConstant.CHANNEL_BASE_URL
                        + config.getApiVersion() + "/"
                        + config.getPhoneNumberId()
                        + EngineConstant.CHANNEL_MESSAGE_SUFFIX;
    }

    public static HttpHeaders getHeaders(WhatsAppConfig config) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getAccessToken());
        return headers;
    }

    public static Map<String, Object> getBaseInteractivePayload(BaseInteractiveMessage msg) {
        Map<String, Object> data = new HashMap<>(Map.of("body", Map.of("text", msg.getBody())));

        if(msg.getTitle() != null) {
            data.put("header", Map.of("type", "text", "text", msg.getTitle()));
        }

        if(msg.getFooter() != null) {
            data.put("footer", Map.of("text", msg.getFooter()));
        }

        return data;
    }

    public static Map<String, Object> getCommonPayload(
            String recipient,
            PayloadType type,
            String previousMessageId
    ) {
        Map<String, Object> payload = new HashMap<>(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", recipient,
                "type", type.name().toLowerCase()
        ));

        if(previousMessageId != null && !previousMessageId.isEmpty()) {
            payload.put("context", Map.of("message_id", previousMessageId));
        }

        return payload;
    }

    public static ListSectionType detectListSectionType(LinkedHashMap<String, Object> sectionData) {
        final String kTitleKey = "title";

        boolean hasSectionTitles = !sectionData.isEmpty() && sectionData.values().stream().allMatch(value ->
                value instanceof Map && ((Map<?, ?>) value)
                        .values()
                        .stream()
                        .allMatch(
                                innerValue -> innerValue instanceof Map && ((Map<?, ?>) innerValue).containsKey(kTitleKey)
                        )
        );

        if(hasSectionTitles) return ListSectionType.SECTION_TITLES;

        boolean allAreRows = sectionData.size() <= 10 && sectionData.values().stream().allMatch(value ->
                value instanceof Map && ((Map<?, ?>) value).containsKey(kTitleKey));

        if(allAreRows) return ListSectionType.ROWS_ONLY;

        return ListSectionType.INVALID;
    }


    public static boolean isValidRequestResponse(String payload) {
        try {
            JsonNode rootNode = SerializeUtils.readStringAsTree(payload);
            String messagingProduct = rootNode.path("messaging_product").asText();
            if(!"whatsapp".equals(messagingProduct)) return false;

            // Check if messages has 1 entry with an id starting with "wamid"
            JsonNode messagesNode = rootNode.path("messages");
            if(messagesNode.isArray() && messagesNode.size() == 1) {
                return messagesNode.get(0).path("id").asText().startsWith("wamid");
            }

            return false;
        } catch (Exception err) {
            return false;
        }
    }

    static public boolean isValidWebhookMessage(Map<String, Object> map) {
        if(map.containsKey("object") && map.get("object").equals("whatsapp_business_account")) {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) map.get("entry");
            for (Map<String, Object> entry : entries) {
                final List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                Map<String, Object> valueMap = changes.get(0);
                Map<String, Object> innerValue = (Map<String, Object>) valueMap.get("value");
                if(innerValue.containsKey("messaging_product") && innerValue.get("messaging_product").equals("whatsapp")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static SupportedMessageType isValidSupportedMessageType(Map<String, Object> message) {
        if(!message.containsKey("type")) return new SupportedMessageType(false, null, null);

        var type = message.get("type").toString().toLowerCase();
        var isSupported = supportedTypes.contains(type);

        if(!isSupported) return new SupportedMessageType(false, null, null);

        switch (type) {
            case "interactive" -> {
                var intr = (Map<String, Object>) message.get("interactive");
                var intrType = intr.get("type").toString();

                return switch (intrType) {
                    case "button_reply" ->
                            new SupportedMessageType(true, WebhookResponseMessageType.INTERACTIVE, WebhookIntrMsgType.BUTTON_REPLY);
                    case "nfm_reply" ->
                            new SupportedMessageType(true, WebhookResponseMessageType.INTERACTIVE, WebhookIntrMsgType.NFM_REPLY);
                    case "list_reply" ->
                            new SupportedMessageType(true, WebhookResponseMessageType.INTERACTIVE, WebhookIntrMsgType.LIST_REPLY);
                    default -> new SupportedMessageType(false, null, null);
                };
            }
            case "document" -> {
                return new SupportedMessageType(true, WebhookResponseMessageType.DOCUMENT, null);
            }
            case "button" -> {
                return new SupportedMessageType(true, WebhookResponseMessageType.BUTTON, null);
            }
            case "location" -> {
                return new SupportedMessageType(true, WebhookResponseMessageType.LOCATION, null);
            }
            case "unknown" -> {
                return new SupportedMessageType(true, WebhookResponseMessageType.UNKNOWN, null);
            }
            case "image" -> {
                return new SupportedMessageType(true, WebhookResponseMessageType.IMAGE, null);
            }
            case "audio", "video", "sticker" -> {
                return new SupportedMessageType(true, WebhookResponseMessageType.MEDIA, null);
            }
        }

        return new SupportedMessageType(true, WebhookResponseMessageType.TEXT, null);
    }

    /**
     * Compute only the needed message input values from webhook
     * <p>
     * If text -> get the string value
     * If button -> get the button value
     * If list -> get list id
     * <p>
     * On unprocessable messages like location and flows, return interactive object data
     */
    public static UserInput getUserInput(Webhook message, String stage) {
//        TODO: handle incoming media message bodies differently

        return switch (message.type().type()) {
            case TEXT -> new UserInput(
                    ((Map) message.message().get(WebhookResponseMessageType.TEXT.name().toLowerCase())).get("body").toString(),
                    null
            );
            case BUTTON -> new UserInput(
                    ((Map) message.message().get(WebhookResponseMessageType.BUTTON.name().toLowerCase())).get("text").toString(),
                    null
            );
            case LOCATION ->
                    new UserInput(null, (Map) message.message().get(WebhookResponseMessageType.LOCATION.name().toLowerCase()));
            case INTERACTIVE -> getListInteractiveIdInput(
                    message.type().interactiveType(),
                    (Map) message.message().get(WebhookResponseMessageType.INTERACTIVE.name().toLowerCase())
            );
            default ->
                    throw new ResponseException(new ResponseError(message.user().waId(), "unsupported response, kindly provide a valid response!", stage));
        };
    }

    public static UserInput getListInteractiveIdInput(WebhookIntrMsgType lType, Map interactiveObj) {
        return switch (lType) {
            case LIST_REPLY, BUTTON_REPLY ->
                    new UserInput(((Map) interactiveObj.get(lType.name().toLowerCase())).get("id").toString(), null);
            case NFM_REPLY -> {
                var flowObj = (Map) interactiveObj.get(lType.name().toLowerCase());
                yield new UserInput(null, SerializeUtils.toMap(flowObj.get("response_json")));
            }
            default -> throw new InternalException("unsupported interactive message type received");
        };
    }

    /*
        if true, we received a whatsapp error message about something
     */
    public static boolean isRequestErrorMessage(Map<String, Object> payload) {
        if(!payload.containsKey("error")) return false;

        Object errorObject = payload.get("error");
        if(!(errorObject instanceof Map<?, ?> errorMap)) return false;

        return errorMap.containsKey("type") && errorMap.containsKey("code");
    }

    public static WaUser computeUser(Map<String, Object> map) {
        String name = null;
        String waId = null;
        String msgId = null;
        Long timestamp = null;

        var entries = (List<Map<String, Object>>) map.get("entry");
        for (Map<String, Object> entry : entries) {
            var changes = (List<Map<String, Object>>) entry.get("changes");
            for (Map<String, Object> change : changes) {
                var value = (Map<String, Object>) change.get("value");
                var contacts = (List<Map<String, Object>>) value.get("contacts");
                for (Map<String, Object> contact : contacts) {
                    var profile = (Map<String, Object>) contact.get("profile");
                    name = (String) profile.get("name");
                    waId = (String) contact.get("wa_id");
                }

                var messages = (List<Map<String, Object>>) value.get("messages");

                for (Map<String, Object> message : messages) {
                    var id = (String) message.get("id");
                    timestamp = Long.valueOf(message.get("timestamp").toString());
                    if(id.startsWith("wamid")) msgId = id;
                }
            }
        }

        Assert.notNull(waId, "waId cannot be null");

        return new WaUser(name, waId, msgId, timestamp);
    }

    public static boolean hasChannelMsgObject(Map<String, Object> map) {
//        sometimes WA sends webhook on msg status change, ignore these
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) map.get("entry");
            for (Map<String, Object> entry : entries) {
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                for (Map<String, Object> change : changes) {
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    return value.containsKey("messages");
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static Map<String, Object> extractMessage(Map<String, Object> map) {
        var entries = (List<Map<String, Object>>) map.get("entry");
        for (Map<String, Object> entry : entries) {
            var changes = (List<Map<String, Object>>) entry.get("changes");
            for (Map<String, Object> change : changes) {
                var value = (Map<String, Object>) change.get("value");
                var messages = (List<Map<String, Object>>) value.get("messages");
                if(!messages.isEmpty()) return messages.get(0);
            }
        }
        return new HashMap<>();
    }

    public static LocalDateTime convertTimestamp(long timestamp) {
        return Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
    }

    public static boolean isOldWebhook(long timestamp, int threshold) {
        return Duration.between(convertTimestamp(timestamp), Utils.currentSystemDate()).abs().toSeconds() > threshold;
    }
}
