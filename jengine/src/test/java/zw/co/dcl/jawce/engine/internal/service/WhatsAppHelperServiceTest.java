package zw.co.dcl.jawce.engine.internal.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.iface.IClientManager;
import zw.co.dcl.jawce.engine.api.utils.Utils;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.configs.WhatsAppConfig;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.model.core.HookRest;
import zw.co.dcl.jawce.engine.model.dto.WebhookProcessorResult;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WhatsAppHelperServiceTest {
    private EngineTestSupport.InMemorySessionManager sessionManager;
    private JawceConfig jawceConfig;
    private WhatsAppConfig whatsAppConfig;

    @BeforeEach
    void setUp() {
        this.sessionManager = new EngineTestSupport.InMemorySessionManager();
        this.jawceConfig = new JawceConfig();
        this.jawceConfig.setStartMenu("START-MENU");
        this.jawceConfig.setHandleSessionInactivity(true);
        this.whatsAppConfig = new WhatsAppConfig();
        this.whatsAppConfig.setAccessToken("access-token");
        this.whatsAppConfig.setPhoneNumberId("phone-id");
    }

    @Test
    void successfulRequestAdvancesSessionAndUpdatesLastActivity() {
        FakeClientManager clientManager = new FakeClientManager(ResponseEntity.ok(validChannelResponse()));
        WhatsAppHelperService service = new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig);

        sessionManager.save("263771234567", SessionConstant.CURRENT_STAGE, "START-MENU");
        service.sendWhatsAppRequest(new WebhookProcessorResult(
                Map.of("type", "text"),
                "REPORT",
                "263771234567",
                true
        ));

        assertEquals("START-MENU", sessionManager.get("263771234567", SessionConstant.PREV_STAGE));
        assertEquals("REPORT", sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
        assertNotNull(sessionManager.get("263771234567", SessionConstant.LAST_ACTIVITY_KEY));
    }

    @Test
    void failedRequestRollsBackToPreviousStage() {
        FakeClientManager clientManager = new FakeClientManager(new RuntimeException("boom"));
        WhatsAppHelperService service = new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig);

        sessionManager.save("263771234567", SessionConstant.PREV_STAGE, "START-MENU");
        sessionManager.save("263771234567", SessionConstant.CURRENT_STAGE, "REPORT");

        assertThrows(InternalException.class, () -> service.sendWhatsAppRequest(new WebhookProcessorResult(
                Map.of("type", "text"),
                "NEXT-STAGE",
                "263771234567",
                true
        )));

        assertEquals("START-MENU", sessionManager.get("263771234567", SessionConstant.CURRENT_STAGE));
    }

    @Test
    void failedRequestOnStartStageClearsSession() {
        FakeClientManager clientManager = new FakeClientManager(new RuntimeException("boom"));
        WhatsAppHelperService service = new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig);

        sessionManager.save("263771234567", SessionConstant.PREV_STAGE, "START-MENU");
        sessionManager.save("263771234567", SessionConstant.CURRENT_STAGE, "START-MENU");
        sessionManager.save("263771234567", SessionConstant.AUTH_SET_KEY, "yes");

        assertThrows(InternalException.class, () -> service.sendWhatsAppRequest(new WebhookProcessorResult(
                Map.of("type", "text"),
                "NEXT-STAGE",
                "263771234567",
                true
        )));

        assertEquals(Map.of(), sessionManager.fetchAll("263771234567"));
    }

    @Test
    void successfulRequestWithoutSessionHandlingStillRecordsLastActivity() {
        FakeClientManager clientManager = new FakeClientManager(ResponseEntity.ok(validChannelResponse()));
        WhatsAppHelperService service = new WhatsAppHelperService(clientManager, sessionManager, jawceConfig, whatsAppConfig);

        service.sendWhatsAppRequest(new WebhookProcessorResult(
                Map.of("type", "reaction"),
                null,
                "263771234567",
                false
        ));

        String lastActivity = sessionManager.get("263771234567", SessionConstant.LAST_ACTIVITY_KEY, String.class);
        assertNotNull(lastActivity);
        assertEquals(lastActivity, Utils.formatZonedDateTime(Utils.parseZonedDateTime(lastActivity)));
    }

    private String validChannelResponse() {
        return "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.mock\"}]}";
    }

    static class FakeClientManager implements IClientManager {
        private final ResponseEntity<String> response;
        private final RuntimeException failure;

        FakeClientManager(ResponseEntity<String> response) {
            this.response = response;
            this.failure = null;
        }

        FakeClientManager(RuntimeException failure) {
            this.response = null;
            this.failure = failure;
        }

        @Override
        public ResponseEntity<String> post(String url, HookRest arg, HttpHeaders headers) {
            return ResponseEntity.status(HttpStatus.OK).body("{}");
        }

        @Override
        public ResponseEntity<String> post(String url, Object payload, HttpHeaders headers) {
            if (failure != null) {
                throw failure;
            }
            return response;
        }

        @Override
        public <T> ResponseEntity<T> request(String url, HttpEntity<?> payload, HttpMethod action, Class<T> response) {
            return ResponseEntity.ok(null);
        }
    }
}
