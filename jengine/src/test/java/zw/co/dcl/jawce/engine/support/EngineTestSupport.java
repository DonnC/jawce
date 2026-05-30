package zw.co.dcl.jawce.engine.support;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.model.core.HookRest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EngineTestSupport {
    private EngineTestSupport() {
    }

    public static Map<String, Object> textWebhook(String input, String messageId) {
        return webhookPayload(
                messageId,
                Map.of(
                        "type", "text",
                        "text", Map.of("body", input)
                )
        );
    }

    public static Map<String, Object> buttonWebhook(String input, String messageId) {
        return webhookPayload(
                messageId,
                Map.of(
                        "type", "interactive",
                        "interactive", Map.of(
                                "type", "button_reply",
                                "button_reply", Map.of(
                                        "id", input,
                                        "title", input
                                )
                        )
                )
        );
    }

    public static Map<String, Object> textWebhook(String input, String messageId, long epochSeconds) {
        return webhookPayload(
                messageId,
                epochSeconds,
                Map.of(
                        "type", "text",
                        "text", Map.of("body", input)
                )
        );
    }

    public static Map<String, Object> buttonWebhook(String input, String messageId, long epochSeconds) {
        return webhookPayload(
                messageId,
                epochSeconds,
                Map.of(
                        "type", "interactive",
                        "interactive", Map.of(
                                "type", "button_reply",
                                "button_reply", Map.of(
                                        "id", input,
                                        "title", input
                                )
                        )
                )
        );
    }

    public static Map<String, Object> webhookPayload(String messageId, Map<String, Object> messageBody) {
        return webhookPayload(messageId, Instant.now().getEpochSecond(), messageBody);
    }

    public static Map<String, Object> webhookPayload(String messageId, long epochSeconds, Map<String, Object> messageBody) {
        Map<String, Object> message = new HashMap<>(messageBody);
        message.put("id", messageId);
        message.put("timestamp", String.valueOf(epochSeconds));

        return Map.of(
                "object", "whatsapp_business_account",
                "entry", List.of(
                        Map.of(
                                "changes", List.of(
                                        Map.of(
                                                "value", Map.of(
                                                        "messaging_product", "whatsapp",
                                                        "contacts", List.of(
                                                                Map.of(
                                                                        "wa_id", "263771234567",
                                                                        "profile", Map.of("name", "Test User")
                                                                )
                                                        ),
                                                        "messages", List.of(message)
                                                )
                                        )
                                )
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> childMap(Map<String, Object> payload, String key) {
        return (Map<String, Object>) payload.get(key);
    }

    public static class RecordingClientManager implements IClientManager {
        private final List<Map<String, Object>> sentPayloads = new ArrayList<>();

        @Override
        public ResponseEntity<String> post(String url, HookRest arg, HttpHeaders headers) {
            return ResponseEntity.ok("{}");
        }

        @Override
        @SuppressWarnings("unchecked")
        public ResponseEntity<String> post(String url, Object payload, HttpHeaders headers) {
            sentPayloads.add((Map<String, Object>) payload);
            return ResponseEntity.ok("{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.mock\"}]}");
        }

        @Override
        public <T> ResponseEntity<T> request(String url, HttpEntity<?> payload, HttpMethod action, Class<T> response) {
            return ResponseEntity.ok(null);
        }

        public Map<String, Object> lastSentPayload() {
            return sentPayloads.get(sentPayloads.size() - 1);
        }

        public int sentCount() {
            return sentPayloads.size();
        }
    }

    public static class InMemorySessionManager implements ISessionManager {
        private final Map<String, Map<String, Object>> store = new ConcurrentHashMap<>();
        private final Map<String, Object> globalStore = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Object>> propsStore = new ConcurrentHashMap<>();

        @Override
        public ISessionManager session(String sessionId) {
            return this;
        }

        @Override
        public void save(String sessionId, String key, Object data) {
            store.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>()).put(key, data);
        }

        @Override
        public Object get(String sessionId, String key) {
            return store.getOrDefault(sessionId, Map.of()).get(key);
        }

        @Override
        public Map<String, Object> fetchAll(String sessionId) {
            return store.getOrDefault(sessionId, Map.of());
        }

        @Override
        public void evict(String sessionId, String key) {
            store.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>()).remove(key);
        }

        @Override
        public void clear(String sessionId) {
            store.put(sessionId, new ConcurrentHashMap<>());
            propsStore.put(sessionId, new ConcurrentHashMap<>());
        }

        @Override
        public void saveAll(String sessionId, Map<String, Object> sessionData) {
            store.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>()).putAll(sessionData);
        }

        @Override
        public void evictAll(String sessionId, List<String> keys) {
            keys.forEach(key -> evict(sessionId, key));
        }

        @Override
        public void evictGlobal(String key) {
            globalStore.remove(key);
        }

        @Override
        public void clear(String sessionId, List<String> retain) {
            Map<String, Object> current = new ConcurrentHashMap<>(fetchAll(sessionId));
            current.keySet().removeIf(key -> !retain.contains(key));
            store.put(sessionId, current);
        }

        @Override
        public boolean evictProp(String sessionId, String propKey) {
            return propsStore.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>()).remove(propKey) != null;
        }

        @Override
        public Object getFromProps(String sessionId, String propKey) {
            return propsStore.getOrDefault(sessionId, Map.of()).get(propKey);
        }

        @Override
        public boolean keyInSession(String sessionId, String key, boolean global) {
            return global ? globalStore.containsKey(key) : fetchAll(sessionId).containsKey(key);
        }

        @Override
        public Map<String, Object> getUserProps(String sessionId) {
            return propsStore.getOrDefault(sessionId, Map.of());
        }

        @Override
        public void saveGlobal(String key, Object data) {
            globalStore.put(key, data);
        }

        @Override
        public void saveProp(String sessionId, String key, Object data) {
            propsStore.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>()).put(key, data);
        }

        @Override
        public <T> T get(String sessionId, String key, Class<T> type) {
            Object value = get(sessionId, key);
            return value == null ? null : type.cast(value);
        }

        @Override
        public <T> T getGlobal(String key, Class<T> type) {
            Object value = globalStore.get(key);
            return value == null ? null : type.cast(value);
        }

        @Override
        public <T> T getFromProps(String sessionId, String propKey, Class<T> propType) {
            Object value = getFromProps(sessionId, propKey);
            return value == null ? null : propType.cast(value);
        }
    }
}
