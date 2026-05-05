package zw.co.dcl.jawce.engine.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;
import zw.co.dcl.jawce.engine.api.iface.ITemplateStorageManager;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;
import zw.co.dcl.jawce.engine.defaults.RestTemplateClientManager;
import zw.co.dcl.jawce.engine.defaults.YmlJsonTemplateStorageManager;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JawceAutoConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void autoConfigProvidesDefaultBeansWhenNoOverridesExist() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("triggers"));
        Path sessionsDir = tempDir.resolve("sessions");

        Files.writeString(
                templatesDir.resolve("templates.yaml"),
                "\"START-MENU\":\n" +
                        "  type: text\n" +
                        "  message: \"Hello\"\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n"
        );
        Files.writeString(triggersDir.resolve("triggers.yaml"), "{}\n");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JawceAutoConfig.class))
                .withPropertyValues(
                        "jawce.start-menu=START-MENU",
                        "jawce.session.dir=" + sessionsDir,
                        "whatsapp.hub-token=test-token",
                        "whatsapp.access-token=test-access",
                        "whatsapp.phone-number-id=test-phone",
                        "template.storage.templates-path=" + templatesDir,
                        "template.storage.triggers-path=" + triggersDir
                )
                .run(context -> {
                    assertNotNull(context.getBean(ISessionManager.class));
                    assertNotNull(context.getBean(IClientManager.class));
                    assertNotNull(context.getBean(ITemplateStorageManager.class));
                    assertInstanceOf(FileSessionManager.class, context.getBean(ISessionManager.class));
                    assertInstanceOf(RestTemplateClientManager.class, context.getBean(IClientManager.class));
                    assertInstanceOf(YmlJsonTemplateStorageManager.class, context.getBean(ITemplateStorageManager.class));
                    assertNotNull(context.getBean(HookService.class));
                    assertNotNull(context.getBean(WebhookProcessor.class));
                    assertNotNull(context.getBean(Worker.class));
                    org.junit.jupiter.api.Assertions.assertFalse(context.containsBean("whatsAppFlowService"));
                });
    }

    @Test
    void autoConfigBacksOffWhenCustomSessionManagerExists() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("custom-templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("custom-triggers"));

        Files.writeString(
                templatesDir.resolve("templates.yaml"),
                "\"START-MENU\":\n" +
                        "  type: text\n" +
                        "  message: \"Hello\"\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n"
        );
        Files.writeString(triggersDir.resolve("triggers.yaml"), "{}\n");

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JawceAutoConfig.class))
                .withUserConfiguration(CustomOverrideConfig.class)
                .withPropertyValues(
                        "jawce.start-menu=START-MENU",
                        "whatsapp.hub-token=test-token",
                        "whatsapp.access-token=test-access",
                        "whatsapp.phone-number-id=test-phone",
                        "template.storage.templates-path=" + templatesDir,
                        "template.storage.triggers-path=" + triggersDir
                )
                .run(context -> {
                    assertInstanceOf(CustomSessionManager.class, context.getBean(ISessionManager.class));
                });
    }

    @Configuration
    static class CustomOverrideConfig {
        @Bean
        ISessionManager sessionManager() {
            return new CustomSessionManager();
        }
    }

    static class CustomSessionManager implements ISessionManager {
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
