package zw.co.dcl.jawce.engine.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.support.StaticApplicationContext;
import zw.co.dcl.jawce.engine.api.utils.Utils;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerEngineResilienceTest {
    @TempDir
    Path tempDir;

    private EngineTestSupport.InMemorySessionManager sessionManager;
    private EngineTestSupport.RecordingClientManager clientManager;
    private Path templatesDir;
    private Path triggersDir;

    @BeforeEach
    void setUp() throws Exception {
        templatesDir = Files.createDirectories(tempDir.resolve("templates"));
        triggersDir = Files.createDirectories(tempDir.resolve("triggers"));

        Files.writeString(
                templatesDir.resolve("init.yaml"),
                "\"START-MENU\":\n" +
                        "  type: button\n" +
                        "  message:\n" +
                        "    title: Title\n" +
                        "    body: Test body\n" +
                        "    footer: jawce\n" +
                        "    buttons:\n" +
                        "      - Button1\n" +
                        "      - Button2\n" +
                        "  routes:\n" +
                        "    \"button1\": \"REPORT\"\n" +
                        "    \"button2\": \"GITHUB-PROFILE\"\n"
        );
        Files.writeString(
                templatesDir.resolve("report.json"),
                "{\n" +
                        "  \"REPORT\": {\n" +
                        "    \"type\": \"text\",\n" +
                        "    \"prop\": \"report\",\n" +
                        "    \"message\": \"Test report\",\n" +
                        "    \"routes\": {\n" +
                        "      \"re:.*\": \"START-MENU\"\n" +
                        "    }\n" +
                        "  },\n" +
                        "  \"GITHUB-PROFILE\": {\n" +
                        "    \"type\": \"text\",\n" +
                        "    \"message\": \"Github profile\",\n" +
                        "    \"routes\": {\n" +
                        "      \"re:.*\": \"START-MENU\"\n" +
                        "    }\n" +
                        "  }\n" +
                        "}\n"
        );
        Files.writeString(
                triggersDir.resolve("triggers.yaml"),
                "\"START-MENU\": \"re:(?i)^(start|hi|hie|menu|hello)$\"\n" +
                        "\"REPORT\": \"re:(?i)^report$\"\n"
        );
    }

    @Test
    void duplicateMessageIdIsSkipped() {
        Worker worker = createWorker(0, false, true, 60);
        Map<String, Object> payload = EngineTestSupport.textWebhook("hello", "wamid-duplicate");

        worker.processWebhook(payload);
        int firstCount = clientManager.sentCount();

        worker.processWebhook(payload);

        assertEquals(firstCount, clientManager.sentCount());
    }

    @Test
    void staleWebhookIsSkipped() {
        Worker worker = createWorker(0, false, true, 30);
        long oldEpochSeconds = Instant.now().minus(120, ChronoUnit.SECONDS).getEpochSecond();

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-old", oldEpochSeconds));

        assertEquals(0, clientManager.sentCount());
    }

    @Test
    void debounceBlocksRapidRepeatProcessing() {
        Worker worker = createWorker(60_000, false, true, 60);

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));
        int firstCount = clientManager.sentCount();

        worker.processWebhook(EngineTestSupport.buttonWebhook("button1", "wamid-2"));

        assertEquals(firstCount, clientManager.sentCount());
        assertEquals("START-MENU", sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
    }

    @Test
    void inactivityResetsSessionAndReturnsExpiryMessage() {
        Worker worker = createWorker(0, false, true, 60);

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));
        sessionManager.save(
                "263771234567",
                SessionConstant.LAST_ACTIVITY_KEY,
                Utils.formatZonedDateTime(Utils.currentSystemDate().minusMinutes(5))
        );

        worker.processWebhook(EngineTestSupport.buttonWebhook("button1", "wamid-2"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        String body = EngineTestSupport.childMap(EngineTestSupport.childMap(payload, "interactive"), "body").get("text").toString();

        assertTrue(body.contains("inactive"));
        assertEquals(null, sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
        assertEquals(null, sessionManager.get("263771234567", SessionConstant.PREV_STAGE));
    }

    private Worker createWorker(long debounceTimeoutMs, boolean emulate, boolean handleSessionInactivity, int webhookTtlSeconds) {
        TemplateStorageProperties storageProperties = new TemplateStorageProperties();
        storageProperties.setTemplatesPath(templatesDir.toString());
        storageProperties.setTriggersPath(triggersDir.toString());

        JawceConfig jawceConfig = new JawceConfig();
        jawceConfig.setStartMenu("START-MENU");
        jawceConfig.setEmulate(emulate);
        jawceConfig.setDebounceTimeoutMs(debounceTimeoutMs);
        jawceConfig.setHandleSessionInactivity(handleSessionInactivity);
        jawceConfig.setSessionTtlMins(1);
        jawceConfig.setWebhookTimestampThresholdSecs(webhookTtlSeconds);

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

        return new Worker(
                publisher,
                whatsAppConfig,
                jawceConfig,
                whatsAppHelperService,
                webhookProcessor,
                sessionManager
        );
    }
}
