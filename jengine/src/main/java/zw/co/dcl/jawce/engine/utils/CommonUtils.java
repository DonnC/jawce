package zw.co.dcl.jawce.engine.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.enums.ListSectionType;
import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.enums.WebhookIntrMsgType;
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.model.DefaultHookArgs;
import zw.co.dcl.jawce.engine.model.dto.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtils {
    private static final Logger logger = LoggerFactory.getLogger(CommonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
    private static final String MUSTACHE_PATTERN = "\\{\\{\\s*(\\w+)\\s*}}";

    public static Map<String, Object> getStaticPayload(
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

    public static boolean containsMustacheVariables(String text) {
        return isRegexPatternMatch(MUSTACHE_PATTERN, text);
    }

    public static List<String> extractMustacheVariables(String text) {
        List<String> variables = new ArrayList<>();
        Matcher matcher = Pattern.compile(MUSTACHE_PATTERN).matcher(text);

        while (matcher.find()) {
            variables.add(matcher.group(1).trim());
        }
        return variables;
    }

    /**
     * shortcuts starts with
     * <p>
     * p.varName for prop data
     * <p>
     * s.varName for general session data
     */
    public static List<String> extractShortcutMustacheVariables(String text) {
        List<String> shortcutVar = new ArrayList<>();

        if(containsMustacheVariables(text)) {
            List<String> variables = extractMustacheVariables(text);

            variables.forEach(shortVar -> {
                if(shortVar.startsWith("p.") || shortVar.startsWith("s.")) {
                    shortcutVar.add(shortVar);
                }
            });
        }

        return shortcutVar;
    }

    public static ShortcutVar parseShortcutVar(String input) {
        // Split the input on the colon (:) to determine if a class type is provided
        String[] parts = input.split(":");

        String name = extractName(parts[0]);

        Class<?> classz = parts.length > 1 ? mapToClass(parts[1]) : null;

        return new ShortcutVar(name, classz);
    }

    private static String extractName(String fullName) {
        int lastDotIndex = fullName.lastIndexOf('.');
        return lastDotIndex != -1 ? fullName.substring(lastDotIndex + 1) : fullName;
    }

    private static Class<?> mapToClass(String className) {
        return switch (className.trim()) {
            case "String" -> String.class;
            case "Integer" -> Integer.class;
            case "Long" -> Long.class;
            case "Double" -> Double.class;
            case "Boolean" -> Boolean.class;
            case "List" -> List.class;
            case "Map" -> Map.class;
            default -> null;
        };
    }

    public static String formatZonedDateTime(ZonedDateTime dateTime) {
        return dateTime.format(dtFormatter);
    }

    public static ZonedDateTime parseZonedDateTime(String dateTimeString) {
        return ZonedDateTime.parse(dateTimeString, dtFormatter);
    }

    public static ZonedDateTime currentSystemDate() {
        return ZonedDateTime.now(ZoneId.systemDefault());
    }

    public static String getSessionExpiryTime(Long expiryInMins) {
        return formatZonedDateTime(currentSystemDate().plusMinutes(expiryInMins));
    }

    public static boolean hasSessionExpired(String sessionTime) {
        if(sessionTime == null) return true;
        return currentSystemDate().isAfter(parseZonedDateTime(sessionTime));
    }

    public static boolean hasInteractionExpired(String lastInteractionTime, int maxInteractionInMins) {
        if(lastInteractionTime == null) return false;
        return Duration.between(parseZonedDateTime(lastInteractionTime), currentSystemDate()).abs().toMinutes() > maxInteractionInMins;
    }

    public static boolean isRegexInput(String input) {
        return input.startsWith(EngineConstants.TPL_REGEX_PLACEHOLDER_KEY);
    }

    public static String getRegexPattern(String regex) {
        if(!isRegexInput(regex)) throw new EngineInternalException("expected a valid regex pattern");

        String[] parts = regex.split(EngineConstants.TPL_REGEX_PLACEHOLDER_KEY);

        if(parts.length == 2) return parts[1];

        throw new EngineInternalException("invalid regex format");
    }

    public static String createEngineErrorMsg(String stage, String recipient, String msg) {
        return stage.trim() + EngineConstants.ENGINE_EXC_MSG_SPLITTER + recipient.trim() + EngineConstants.ENGINE_EXC_MSG_SPLITTER + msg.trim();
    }

    public static String getPreviousMessageId(Map<String, Object> tpl) {
        return (String) tpl.getOrDefault(EngineConstants.TPL_REPLY_MESSAGE_ID_KEY, null);
    }

    /**
     * For channel error response ->
     * <p>
     * stage | recipient | message
     *
     * @param splitter regex pattern or char to split the given input
     * @param input    Input data to split using [splitter]
     * @return DataDatumDTO
     */
    public static DataDatumDTO getDataDatumArgs(String splitter, String input) {
        String[] parts = input.split(splitter);

        if(parts.length == 2) {
            return new DataDatumDTO(parts[0], parts[1], null);
        } else if(parts.length == 3) {
            return new DataDatumDTO(parts[0], parts[1], parts[2]);
        }

        throw new EngineInternalException("invalid format to parse");
    }

    public static boolean isRegexPatternMatch(String regexPattern, String text) {
        return Pattern.compile(regexPattern).matcher(text).find();
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

    public static Object convertResponseToHookObj(String response) {
        return fromJsonMap(objectToMap(response), DefaultHookArgs.class);
    }

    static public Map<String, Object> linkedHashToMap(LinkedHashMap lhm) {
        try {
            return fromJsonMap(lhm, Map.class);
        } catch (Exception e) {
            logger.error("Failed to convert linkedHashMap: {}", e.getMessage(), e);
            return lhm;
        }
    }

    public static boolean isValidChannelResponse(String payload) {
        try {
            JsonNode rootNode = objectMapper.readTree(payload);
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

    static public boolean isValidChannelMessage(Map<String, Object> map) {
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

        String type = message.get("type").toString().toLowerCase();

        var isSupported = type.equals("text") || type.equals("interactive") || type.equals("button") ||
                type.equals("unknown") || type.equals("location") || type.equals("image") || type.equals("document")
                || type.equals("video") || type.equals("sticker") || type.equals("audio");

        if(!isSupported) return new SupportedMessageType(false, null, null);

        if(type.equals("interactive")) {
            var intr = (Map<String, Object>) message.get("interactive");
            String intrType = intr.get("type").toString();

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

        if(type.equals("document")) {
            return new SupportedMessageType(true, WebhookResponseMessageType.DOCUMENT, null);
        }

        if(type.equals("button")) {
            return new SupportedMessageType(true, WebhookResponseMessageType.BUTTON, null);
        }

        if(type.equals("location")) {
            return new SupportedMessageType(true, WebhookResponseMessageType.LOCATION, null);
        }

        if(type.equals("unknown")) {
            return new SupportedMessageType(true, WebhookResponseMessageType.UNKNOWN, null);
        }

        if(type.equals("image")) {
            return new SupportedMessageType(true, WebhookResponseMessageType.IMAGE, null);
        }

        if(type.equals("audio") || type.equals("video") || type.equals("sticker")) {
            return new SupportedMessageType(true, WebhookResponseMessageType.MEDIA, null);
        }

        return new SupportedMessageType(true, WebhookResponseMessageType.TEXT, null);
    }

    public static ChannelUserInput getListInteractiveIdInput(WebhookIntrMsgType lType, Map interactiveObj) {
        return switch (lType) {
            case LIST_REPLY, BUTTON_REPLY ->
                    new ChannelUserInput(((Map) interactiveObj.get(lType.name().toLowerCase())).get("id").toString(), null);
            case NFM_REPLY -> {
                var flowObj = (Map) interactiveObj.get(lType.name().toLowerCase());
                var flowResponsePayload = objectToMap(flowObj.get("response_json"));
                yield new ChannelUserInput(flowResponsePayload.get("screen").toString(), flowResponsePayload);
            }
            default -> throw new EngineInternalException("unsupported interactive message type received");
        };
    }

    /*
        if true, we received a whatsapp error message about something
     */
    public static boolean isChannelErrorMessage(Map<String, Object> payload) {
        if(!payload.containsKey("error")) return false;

        Object errorObject = payload.get("error");
        if(!(errorObject instanceof Map<?, ?> errorMap)) return false;

        return errorMap.containsKey("type") && errorMap.containsKey("code");
    }

    public static WaCurrentUser extractWaCurrentUserObj(Map<String, Object> map) {
        String name = null;
        String waId = null;
        String msgId = null;
        Long timestamp = null;

        List<Map<String, Object>> entries = (List<Map<String, Object>>) map.get("entry");
        for (Map<String, Object> entry : entries) {
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
            for (Map<String, Object> change : changes) {
                Map<String, Object> value = (Map<String, Object>) change.get("value");
                List<Map<String, Object>> contacts = (List<Map<String, Object>>) value.get("contacts");
                for (Map<String, Object> contact : contacts) {
                    Map<String, Object> profile = (Map<String, Object>) contact.get("profile");
                    name = (String) profile.get("name");
                    waId = (String) contact.get("wa_id");
                }

                List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

                for (Map<String, Object> message : messages) {
                    String id = (String) message.get("id");
                    timestamp = Long.valueOf(message.get("timestamp").toString());
                    if(id.startsWith("wamid")) msgId = id;
                }
            }
        }

        assert waId != null;

        return new WaCurrentUser(name, waId, msgId, timestamp);
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

    public static Map<String, Object> extractWaMessage(Map<String, Object> map) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) map.get("entry");
        for (Map<String, Object> entry : entries) {
            List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
            for (Map<String, Object> change : changes) {
                Map<String, Object> value = (Map<String, Object>) change.get("value");
                List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                if(!messages.isEmpty()) return messages.get(0);
            }
        }
        return Map.of();
    }


    public static LocalDateTime convertTimestamp(long timestamp) {
        return Instant.ofEpochSecond(timestamp).atZone(ZoneOffset.systemDefault()).toLocalDateTime();
    }

    public static boolean isOldWebhook(long timestamp, int threshold) {
        return Duration.between(
                        convertTimestamp(timestamp),
                        currentSystemDate()
                )
                .abs()
                .toSeconds() > threshold;
    }

    /**
     * if engine has restricted origin,
     * pass the Pattern to match the user number
     *
     * @param patterns  list of patterns to verify
     * @param recipient to check if it matches provided patterns
     * @return boolean
     */
    public static boolean isAllowedChannelOrigin(List<Pattern> patterns, String recipient) {
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(recipient);
            if(matcher.matches()) return true;  // Input matches at least one pattern
        }
        return false;  // Input does not match any pattern
    }

    public static <T> T fromJsonMap(Map linkedHashMap, Class<T> type) {
        return objectMapper.convertValue(linkedHashMap, type);
    }

    static public Map<String, Object> objectToMap(Object object) {
        try {
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            if(object instanceof String content) {
                return objectMapper.readValue(content, new TypeReference<Map<String, Object>>() {
                });
            }

            String json = objectMapper.writeValueAsString(object);
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            logger.warn("[ENGINE] failed to convert obj to map: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    static public String toJsonString(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }

    public static String parseHtmlEncodedContent(String message) {
        try {
            return URLDecoder.decode(message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return message;
        }
    }
}
