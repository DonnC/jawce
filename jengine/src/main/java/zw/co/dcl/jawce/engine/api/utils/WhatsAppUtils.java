package zw.co.dcl.jawce.engine.api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.engine.api.enums.MessageTypeEnum;
import zw.co.dcl.jawce.engine.api.enums.PayloadType;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.exceptions.WhatsAppException;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.internal.dto.UserInput;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.ResponseStructure;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class WhatsAppUtils {
    static final Map<String, MessageTypeEnum> typeMap = Map.ofEntries(
            Map.entry("text", MessageTypeEnum.TEXT),
            Map.entry("button", MessageTypeEnum.BUTTON),
            Map.entry("image", MessageTypeEnum.IMAGE),
            Map.entry("document", MessageTypeEnum.DOCUMENT),
            Map.entry("video", MessageTypeEnum.VIDEO),
            Map.entry("audio", MessageTypeEnum.AUDIO),
            Map.entry("reaction", MessageTypeEnum.REACTION),
            Map.entry("location", MessageTypeEnum.LOCATION),
            Map.entry("contacts", MessageTypeEnum.CONTACTS),
            Map.entry("unknown", MessageTypeEnum.UNKNOWN),
            Map.entry("unsupported", MessageTypeEnum.UNSUPPORTED),
            Map.entry("order", MessageTypeEnum.ORDER),
            Map.entry("interactive", MessageTypeEnum.INTERACTIVE),
            Map.entry("sticker", MessageTypeEnum.STICKER),
            Map.entry("list_reply", MessageTypeEnum.INTERACTIVE_LIST),
            Map.entry("button_reply", MessageTypeEnum.INTERACTIVE_BUTTON),
            Map.entry("nfm_reply", MessageTypeEnum.INTERACTIVE_FLOW)
    );

    public static Optional<WaUser> getUser(Map<String, Object> webhookPayload, int webhookTtl) {
        if(isRequestErrorMessage(webhookPayload)) {
            throw new WhatsAppException(webhookPayload.toString());
        }
        if(!isValidWebhookMessage(webhookPayload)) {
            throw new InternalException("invalid channel message received");
        }

        if(hasChannelMsgObject(webhookPayload)) {
            var msgData = extractMessage(webhookPayload);

            var responseStructure = getResponseStructure(msgData);
            if(responseStructure.type().equals(MessageTypeEnum.UNSUPPORTED)) {
                throw new InternalException("unsupported message response");
            }

            WaUser waUser = computeUser(webhookPayload);

            MDC.put(EngineConstant.MDC_WA_ID_KEY, waUser.waId());
            MDC.put(EngineConstant.MDC_WA_NAME_KEY, waUser.name());

            if(isOldWebhook(waUser.timestamp(), webhookTtl)) {
                log.warn("Old webhook received: {}. Discarded!", convertTimestamp(waUser.timestamp()));
                return Optional.empty();
            }

            return Optional.of(waUser);
        }

        log.warn("No message obj, ignoring..");
        return Optional.empty();
    }

    public static String getMediaIdUrl(WhatsAppConfig config, String mediaId) {
        return EngineConstant.CHANNEL_BASE_URL
                + config.getApiVersion() + "/"
                + mediaId;
    }

    public static String getUrl(WhatsAppConfig config, boolean isMedia) {
        var suffix = isMedia ? EngineConstant.CHANNEL_MEDIA_SUFFIX : EngineConstant.CHANNEL_MESSAGE_SUFFIX;
        return EngineConstant.CHANNEL_BASE_URL
                + config.getApiVersion() + "/"
                + config.getPhoneNumberId()
                + suffix;
    }

    public static HttpHeaders getHeaders(WhatsAppConfig config, boolean isMedia, boolean skipContentType) {
        var headers = new HttpHeaders();
        if(!skipContentType) {
            headers.setContentType(isMedia ? MediaType.MULTIPART_FORM_DATA : MediaType.APPLICATION_JSON);
        }
        headers.setBearerAuth(config.getAccessToken());
        return headers;
    }

    public static Map<String, Object> getBaseInteractivePayload(BaseInteractiveMessage msg) {
        Map<String, Object> data = new HashMap<>(Map.of("body", Map.of("text", msg.getBody())));

        if(msg.getTitle() != null) {
            data.put("header", Map.of("type", "text", "text", msg.getTitle()));
        }

        if(msg instanceof ButtonMessage btn) {
            if(btn.getHeader() != null) {
                Map<String, Object> header = new HashMap<>(Map.of("type", btn.getHeader().getType()));
                if(btn.getHeader().getMediaId() != null) {
                    header.put(btn.getHeader().getType(), Map.of("id", btn.getHeader().getMediaId()));
                } else if(btn.getHeader().getUrl() != null) {
                    header.put(btn.getHeader().getType(), Map.of("link", btn.getHeader().getUrl()));
                }

                data.put("header", header);
            }
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
        } catch (Exception err) {
            log.error("Failed to check for valid response: {}", err.getMessage());
        }
        return false;
    }

    public static String getSingleResponseValue(String payload, String key) {
        try {
            JsonNode root = SerializeUtils.readStringAsTree(payload);
            JsonNode idNode = root.path(key);
            if(!idNode.isMissingNode() && !idNode.isNull()) {
                return idNode.asText();
            }
            throw new RuntimeException("Operation succeeded but response has no " + key);
        } catch (Exception e) {
            log.error("Failed to get response {}: {}", key, e.getMessage());
        }

        return null;
    }

    static public boolean isValidWebhookMessage(Map<String, Object> map) {
        if(map.containsKey("object") && map.get("object").equals("whatsapp_business_account")) {
            var entries = (List<Map<String, Object>>) map.get("entry");
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

    public static ResponseStructure getResponseStructure(Map<String, Object> message) {
        log.info("GET RESPONSE STRUCTURE RECEIVED MESSAGE: {}", message);
        if(!message.containsKey("type")) return new ResponseStructure(MessageTypeEnum.UNSUPPORTED, null);

        var messageType = message.get("type").toString().toLowerCase();
        var isSupported = typeMap.containsKey(messageType);

        if(!isSupported) return new ResponseStructure(MessageTypeEnum.UNSUPPORTED, null);

        MessageTypeEnum type = typeMap.get(messageType);

        if(type.equals(MessageTypeEnum.INTERACTIVE)) {
            var intr = (Map<String, Object>) message.get("interactive");
            var intrType = intr.get("type").toString();
            type = typeMap.getOrDefault(intrType, MessageTypeEnum.UNSUPPORTED);

            if(!type.equals(MessageTypeEnum.INTERACTIVE)) {
                if(intrType.equals("nfm_reply")) {
                    var response = (Map) intr.get(intrType);
                    return new ResponseStructure(type, SerializeUtils.toMap(response.get("response_json")));
                }

                return new ResponseStructure(type, (Map) intr.get(intrType));
            }

            return new ResponseStructure(type, (Map) message.get("interactive"));
        }

        log.info("FOUND SUPPORTED MESSAGE MAPPED: {} >>> {}", type, message.get(messageType));

        return new ResponseStructure(type, (Map) message.get(messageType));
    }

    /**
     * Compute only the needed message input values from webhook
     * <p>
     * On unprocessable messages like location and flows, return interactive object data
     */
    public static UserInput getUserInput(ResponseStructure payload) {
        return switch (payload.type()) {
            case TEXT -> new UserInput(payload.body().get("body").toString(), null);
            case BUTTON, INTERACTIVE_BUTTON, INTERACTIVE_LIST -> {
                if(payload.body().containsKey("text")) {
                    yield new UserInput(payload.body().get("text").toString(), null);
                } else {
                    yield new UserInput(payload.body().get("id").toString(), payload.body());
                }
            }
            case INTERACTIVE_FLOW -> new UserInput((String) payload.body().get("screen"), payload.body());
            default -> new UserInput(null, payload.body());
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
