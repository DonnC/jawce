package zw.co.dcl.jawce.engine.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.StaticApplicationContext;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.defaults.YmlJsonTemplateStorageManager;
import zw.co.dcl.jawce.engine.internal.service.FlowHookRegistry;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.model.history.HistoryEventType;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerHistoryTest {
    @TempDir
    Path tempDir;

    private EngineTestSupport.InMemorySessionManager sessionManager;
    private EngineTestSupport.RecordingClientManager clientManager;
    private EngineTestSupport.CollectingEventPublisher eventPublisher;
    private Worker worker;

    @BeforeEach
    void setUp() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("triggers"));

        Files.writeString(
                templatesDir.resolve("templates.yaml"),
                "\"START-MENU\":\n" +
                        "  type: button\n" +
                        "  message:\n" +
                        "    title: Start\n" +
                        "    body: Choose\n" +
                        "    buttons:\n" +
                        "      - Button1\n" +
                        "  routes:\n" +
                        "    \"button1\": \"REPORT\"\n" +
                        "\n" +
                        "\"REPORT\":\n" +
                        "  type: text\n" +
                        "  message: Report stage\n" +
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
        this.eventPublisher = new EngineTestSupport.CollectingEventPublisher();

        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        FlowHookRegistry flowHookRegistry = new FlowHookRegistry(applicationContext);
        HookService hookService = new HookService(clientManager, jawceConfig, applicationContext, flowHookRegistry);
        YmlJsonTemplateStorageManager templateStorageManager = new YmlJsonTemplateStorageManager(storageProperties, flowHookRegistry);
        WhatsAppHelperService whatsAppHelperService = new WhatsAppHelperService(
                clientManager,
                sessionManager,
                jawceConfig,
                whatsAppConfig,
                eventPublisher.historyEventPublisher()
        );
        WebhookProcessor webhookProcessor = new WebhookProcessor(
                hookService,
                sessionManager,
                templateStorageManager,
                jawceConfig,
                whatsAppHelperService,
                eventPublisher.historyEventPublisher()
        );

        this.worker = new Worker(
                eventPublisher,
                whatsAppConfig,
                jawceConfig,
                whatsAppHelperService,
                webhookProcessor,
                sessionManager,
                eventPublisher.historyEventPublisher()
        );
    }

    @Test
    void successfulFlowPublishesInboundStageAndOutboundHistory() {
        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));

        List<HistoryEventType> eventTypes = eventPublisher.historyEvents().stream()
                .map(event -> event.getType())
                .toList();

        assertEquals(
                List.of(
                        HistoryEventType.INBOUND_RECEIVED,
                        HistoryEventType.STAGE_RESOLVED,
                        HistoryEventType.OUTBOUND_GENERATED,
                        HistoryEventType.OUTBOUND_SENT
                ),
                eventTypes
        );
    }

    @Test
    void duplicateInboundPublishesSkipHistory() {
        var payload = EngineTestSupport.textWebhook("hello", "wamid-dup");

        worker.processWebhook(payload);
        worker.processWebhook(payload);

        assertTrue(eventPublisher.historyEvents().stream()
                .anyMatch(event -> event.getType() == HistoryEventType.INBOUND_SKIPPED_DUPLICATE));
    }
}
