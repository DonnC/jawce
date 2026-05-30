package zw.co.dcl.jawce.engine.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.support.StaticApplicationContext;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.TemplateStorageProperties;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.defaults.YmlJsonTemplateStorageManager;
import zw.co.dcl.jawce.engine.internal.service.FlowHookRegistry;
import zw.co.dcl.jawce.engine.internal.service.HookService;
import zw.co.dcl.jawce.engine.internal.service.WebhookProcessor;
import zw.co.dcl.jawce.engine.internal.service.WhatsAppHelperService;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;
import zw.co.dcl.jawce.engine.support.NamedHookBeans;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkerEngineHooksTest {
    @TempDir
    Path tempDir;

    private EngineTestSupport.InMemorySessionManager sessionManager;
    private EngineTestSupport.RecordingClientManager clientManager;
    private Worker worker;
    private EngineTestSupport.CollectingEventPublisher eventPublisher;

    @BeforeEach
    void setUp() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("triggers"));

        Files.writeString(
                templatesDir.resolve("templates.yaml"),
                "\"START-MENU\":\n" +
                        "  type: button\n" +
                        "  on-receive: zw.co.dcl.jawce.engine.support.TestHooks.startOnReceive\n" +
                        "  message:\n" +
                        "    title: Start\n" +
                        "    body: Choose what to test\n" +
                        "    buttons:\n" +
                        "      - Hook\n" +
                        "      - Dynamic\n" +
                        "      - Continue\n" +
                        "  routes:\n" +
                        "    \"hook\": \"HOOK-TARGET\"\n" +
                        "    \"dynamic\": \"DYNAMIC-TEXT\"\n" +
                        "    \"continue\": \"HOOK-TARGET\"\n" +
                        "\n" +
                        "\"HOOK-TARGET\":\n" +
                        "  type: text\n" +
                        "  on-generate: zw.co.dcl.jawce.engine.support.TestHooks.nextOnGenerate\n" +
                        "  message: Hook target reached\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n" +
                        "\n" +
                        "\"DYNAMIC-TEXT\":\n" +
                        "  type: text\n" +
                        "  template: zw.co.dcl.jawce.engine.support.TestHooks.renderName\n" +
                        "  message: Hello {{ name }}\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n" +
                        "\n" +
                        "\"REPORT\":\n" +
                        "  type: text\n" +
                        "  message: Report stage\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n"
        );
        Files.writeString(
                triggersDir.resolve("triggers.yaml"),
                "\"START-MENU\": \"re:(?i)^(start|hi|hie|menu|hello)$\"\n"
        );

        this.worker = createWorker(templatesDir, triggersDir, false);
    }

    @Test
    void onReceiveRunsBeforeOnGenerateDuringTransition() {
        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));
        worker.processWebhook(EngineTestSupport.buttonWebhook("hook", "wamid-2"));

        assertEquals(List.of("on_receive", "on_receive", "on_generate"), sessionManager.getGlobal("events", List.class));
        assertEquals("HOOK-TARGET", sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
    }

    @Test
    void templateHookRendersDynamicMessage() {
        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));
        worker.processWebhook(EngineTestSupport.buttonWebhook("dynamic", "wamid-2"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        assertEquals("text", payload.get("type"));
        assertEquals("Hello TDD", EngineTestSupport.childMap(payload, "text").get("body"));
    }

    @Test
    void routerHookRedirectsNextStage() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("router-templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("router-triggers"));

        Files.writeString(
                templatesDir.resolve("templates.yaml"),
                "\"START-MENU\":\n" +
                        "  type: button\n" +
                        "  router: zw.co.dcl.jawce.engine.support.TestHooks.routeToReport\n" +
                        "  message:\n" +
                        "    title: Start\n" +
                        "    body: Router test\n" +
                        "    buttons:\n" +
                        "      - Continue\n" +
                        "  routes:\n" +
                        "    \"continue\": \"HOOK-TARGET\"\n" +
                        "\n" +
                        "\"HOOK-TARGET\":\n" +
                        "  type: text\n" +
                        "  message: Hook target reached\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n" +
                        "\n" +
                        "\"REPORT\":\n" +
                        "  type: text\n" +
                        "  message: Report stage\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n"
        );
        Files.writeString(
                triggersDir.resolve("triggers.yaml"),
                "\"START-MENU\": \"re:(?i)^(start|hi|hie|menu|hello)$\"\n"
        );

        worker = createWorker(templatesDir, triggersDir, false);

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));
        worker.processWebhook(EngineTestSupport.buttonWebhook("continue", "wamid-2"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        assertEquals("text", payload.get("type"));
        assertEquals("Report stage", EngineTestSupport.childMap(payload, "text").get("body"));
        assertEquals("REPORT", sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
    }

    @Test
    void namedTypedHooksCanBeResolvedWithoutReflectionSyntax() throws Exception {
        Path templatesDir = Files.createDirectories(tempDir.resolve("named-templates"));
        Path triggersDir = Files.createDirectories(tempDir.resolve("named-triggers"));

        Files.writeString(
                templatesDir.resolve("templates.yaml"),
                "\"START-MENU\":\n" +
                        "  type: button\n" +
                        "  on-receive: namedReceive\n" +
                        "  message:\n" +
                        "    title: Start\n" +
                        "    body: Choose\n" +
                        "    buttons:\n" +
                        "      - Continue\n" +
                        "      - Template\n" +
                        "  routes:\n" +
                        "    \"continue\": \"NEXT-STAGE\"\n" +
                        "    \"template\": \"TEMPLATE-STAGE\"\n" +
                        "\n" +
                        "\"NEXT-STAGE\":\n" +
                        "  type: text\n" +
                        "  on-generate: namedGenerate\n" +
                        "  message: Hello {{ name }}\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n" +
                        "\n" +
                        "\"TEMPLATE-STAGE\":\n" +
                        "  type: text\n" +
                        "  template: namedTemplate\n" +
                        "  message: Hello {{ name }}\n" +
                        "  routes:\n" +
                        "    \"re:.*\": \"START-MENU\"\n" +
                        "\n"
        );
        Files.writeString(
                triggersDir.resolve("triggers.yaml"),
                "\"START-MENU\": \"re:(?i)^(start|hi|hie|menu|hello)$\"\n"
        );

        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton("namedReceiveHook", NamedHookBeans.NamedReceiveHook.class);
        applicationContext.registerSingleton("namedGenerateHook", NamedHookBeans.NamedGenerateHook.class);
        applicationContext.registerSingleton("namedTemplateHook", NamedHookBeans.NamedTemplateHook.class);
        applicationContext.refresh();

        worker = createWorker(templatesDir, triggersDir, false, applicationContext);

        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-1"));
        worker.processWebhook(EngineTestSupport.buttonWebhook("continue", "wamid-2"));

        Map<String, Object> payload = clientManager.lastSentPayload();
        assertEquals("text", payload.get("type"));
        assertEquals("Hello Named", EngineTestSupport.childMap(payload, "text").get("body"));
        assertEquals(List.of("named_receive", "named_receive"), sessionManager.getGlobal("events", List.class));

        worker = createWorker(templatesDir, triggersDir, false, applicationContext);
        worker.processWebhook(EngineTestSupport.textWebhook("hello", "wamid-3"));
        worker.processWebhook(EngineTestSupport.buttonWebhook("template", "wamid-4"));
        Map<String, Object> templatePayload = clientManager.lastSentPayload();
        assertEquals("Hello NamedTemplate", EngineTestSupport.childMap(templatePayload, "text").get("body"));
    }

    private Worker createWorker(Path templatesDir, Path triggersDir, boolean emulate) {
        return createWorker(templatesDir, triggersDir, emulate, null);
    }

    private Worker createWorker(Path templatesDir, Path triggersDir, boolean emulate, StaticApplicationContext applicationContext) {
        TemplateStorageProperties storageProperties = new TemplateStorageProperties();
        storageProperties.setTemplatesPath(templatesDir.toString());
        storageProperties.setTriggersPath(triggersDir.toString());

        JawceConfig jawceConfig = new JawceConfig();
        jawceConfig.setStartMenu("START-MENU");
        jawceConfig.setEmulate(emulate);
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

        StaticApplicationContext appContext = applicationContext == null ? new StaticApplicationContext() : applicationContext;
        if(!appContext.isActive()) {
            appContext.refresh();
        }

        FlowHookRegistry flowHookRegistry = new FlowHookRegistry(appContext);
        HookService hookService = new HookService(clientManager, jawceConfig, appContext, flowHookRegistry);
        YmlJsonTemplateStorageManager templateStorageManager = new YmlJsonTemplateStorageManager(storageProperties, flowHookRegistry);
        WhatsAppHelperService whatsAppHelperService = new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig, eventPublisher.historyEventPublisher());
        WebhookProcessor webhookProcessor = new WebhookProcessor(
                hookService,
                sessionManager,
                templateStorageManager,
                jawceConfig,
                whatsAppHelperService,
                eventPublisher.historyEventPublisher()
        );
        return new Worker(
                eventPublisher,
                whatsAppConfig,
                jawceConfig,
                whatsAppHelperService,
                webhookProcessor,
                sessionManager,
                eventPublisher.historyEventPublisher()
        );
    }
}
