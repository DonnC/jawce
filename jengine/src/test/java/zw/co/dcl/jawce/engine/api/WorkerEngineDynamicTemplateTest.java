package zw.co.dcl.jawce.engine.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.StaticApplicationContext;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.defaults.YmlJsonTemplateStorageManager;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerEngineDynamicTemplateTest {
    @TempDir
    Path tempDir;

    private EngineTestSupport.InMemorySessionManager sessionManager;
    private EngineTestSupport.RecordingClientManager clientManager;
    private Worker worker;

    @BeforeEach
    void setUp() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("triggers"));

        Files.writeString(
                templatesDir.resolve("dynamic.yaml"),
                "\"START-MENU\":\n" +
                        "  type: text\n" +
                        "  on-generate: \"zw.co.dcl.jawce.engine.support.TestHooks.renderDynamicAccountSelector\"\n" +
                        "  message: \"placeholder\"\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"DONE\"\n" +
                        "\n" +
                        "\"DONE\":\n" +
                        "  type: text\n" +
                        "  message: \"Done\"\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n"
        );

        Files.writeString(
                triggersDir.resolve("triggers.yaml"),
                "\"START-MENU\": \"re:(?i)^(start|hi|hello)$\"\n"
        );

        TemplateStorageProperties storageProperties = new TemplateStorageProperties();
        storageProperties.setTemplatesPath(templatesDir.toString());
        storageProperties.setTriggersPath(triggersDir.toString());

        JawceConfig jawceConfig = new JawceConfig();
        jawceConfig.setStartMenu("START-MENU");
        jawceConfig.setEmulate(true);
        jawceConfig.setDebounceTimeoutMs(0);
        jawceConfig.setHandleSessionInactivity(false);
        jawceConfig.setWebhookTimestampThresholdSecs(0);

        WhatsAppConfig whatsAppConfig = new WhatsAppConfig();
        whatsAppConfig.setHubToken("hub-token");
        whatsAppConfig.setAccessToken("access-token");
        whatsAppConfig.setPhoneNumberId("phone-id");

        this.sessionManager = new EngineTestSupport.InMemorySessionManager();
        this.clientManager = new EngineTestSupport.RecordingClientManager();

        HookService hookService = new HookService(clientManager, jawceConfig, new StaticApplicationContext());
        YmlJsonTemplateStorageManager templateStorageManager = new YmlJsonTemplateStorageManager(storageProperties);
        WhatsAppHelperService whatsAppHelperService = new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig);
        WebhookProcessor webhookProcessor = new WebhookProcessor(
                hookService,
                sessionManager,
                templateStorageManager,
                jawceConfig,
                whatsAppHelperService
        );
        ApplicationEventPublisher publisher = event -> {
        };

        this.worker = new Worker(
                publisher,
                whatsAppConfig,
                jawceConfig,
                whatsAppHelperService,
                webhookProcessor,
                sessionManager
        );
    }

    @Test
    void onGenerateCanReplaceOutgoingTemplateWithButtons() {
        sessionManager.saveGlobal("accounts", List.of("Savings", "Cheque"));

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-buttons"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        Map<String, Object> interactive = EngineTestSupport.childMap(payload, "interactive");
        Map<String, Object> action = EngineTestSupport.childMap(interactive, "action");
        List<?> buttons = (List<?>) action.get("buttons");

        assertEquals("interactive", payload.get("type"));
        assertEquals("button", interactive.get("type"));
        assertEquals(2, buttons.size());
        assertEquals("START-MENU", sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
    }

    @Test
    void onGenerateCanReplaceOutgoingTemplateWithList() {
        sessionManager.saveGlobal("accounts", List.of("Savings", "Cheque", "USD", "Business"));

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-list"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        Map<String, Object> interactive = EngineTestSupport.childMap(payload, "interactive");
        Map<String, Object> action = EngineTestSupport.childMap(interactive, "action");
        List<?> sections = (List<?>) action.get("sections");

        assertEquals("interactive", payload.get("type"));
        assertEquals("list", interactive.get("type"));
        assertEquals("Accounts", action.get("button"));
        assertEquals(1, sections.size());
    }

    @Test
    void onGenerateCanReplaceOutgoingTemplateWithText() {
        sessionManager.saveGlobal(
                "accounts",
                List.of("A1", "A2", "A3", "A4", "A5", "A6", "A7", "A8", "A9", "A10", "A11")
        );

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-text"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        String body = EngineTestSupport.childMap(payload, "text").get("body").toString();

        assertEquals("text", payload.get("type"));
        assertTrue(body.contains("1. A1"));
        assertTrue(body.contains("11. A11"));
    }
}
