package zw.co.dcl.ehailing.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zw.co.dcl.jawce.engine.api.pagination.PaginationSupport;
import zw.co.dcl.jawce.engine.configs.FileSessionProperties;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;
import zw.co.dcl.jawce.engine.model.template.ListTemplate;
import zw.co.dcl.jawce.engine.model.template.TextTemplate;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverSelectionHookTest {
    @TempDir
    Path tempDir;

    private FileSessionManager createSessionManager() {
        FileSessionProperties properties = new FileSessionProperties();
        properties.setDir(tempDir.resolve("sessions").toString());
        return new FileSessionManager(properties);
    }

    @Test
    void rideScenarioRendersButtonsForClosestDrivers() {
        FileSessionManager sessionManager = createSessionManager();
        String sessionId = "263771234567";
        sessionManager.saveProp(sessionId, "rideType", "Ride");

        DriverSelectionHook hook = new DriverSelectionHook();
        Hook arg = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .build();

        Hook result = hook.render(arg);

        assertNotNull(result.getTemplateDynamicBody());
        assertInstanceOf(ButtonTemplate.class, result.getTemplateDynamicBody().getTemplate());

        ButtonTemplate template = (ButtonTemplate) result.getTemplateDynamicBody().getTemplate();
        assertEquals(List.of("Blessing M.", "Rufaro K.", "Nigel T."), template.getMessage().getButtons());
        assertTrue(template.getMessage().getBody().contains("closest drivers"));
    }

    @Test
    void standardScenarioRendersPaginatedList() {
        FileSessionManager sessionManager = createSessionManager();
        String sessionId = "263771234568";
        sessionManager.saveProp(sessionId, "rideType", "Standard");

        DriverSelectionHook hook = new DriverSelectionHook();
        Hook arg = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .build();

        Hook result = hook.render(arg);

        assertInstanceOf(ListTemplate.class, result.getTemplateDynamicBody().getTemplate());

        ListTemplate template = (ListTemplate) result.getTemplateDynamicBody().getTemplate();
        assertEquals("Nearby Drivers", template.getMessage().getTitle());
        assertTrue(template.getMessage().getBody().contains("Page 1 of 2"));
        assertEquals(9, template.getMessage().getSections().get(0).getRows().size());
        assertEquals("drv-201", template.getMessage().getSections().get(0).getRows().get(0).getId());
        assertEquals("availableDrivers::NEXT::1", template.getMessage().getSections().get(0).getRows().get(8).getId());
    }

    @Test
    void luxuryScenarioSupportsTextPaginationAndCapture() {
        FileSessionManager sessionManager = createSessionManager();
        String sessionId = "263771234569";
        sessionManager.saveProp(sessionId, "rideType", "Luxury");

        DriverSelectionHook hook = new DriverSelectionHook();
        Hook arg = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .build();

        Hook firstPage = hook.render(arg);
        assertInstanceOf(TextTemplate.class, firstPage.getTemplateDynamicBody().getTemplate());

        TextTemplate firstTemplate = (TextTemplate) firstPage.getTemplateDynamicBody().getTemplate();
        assertTrue(firstTemplate.getMessage().contains("Page 1 of 2"));
        assertTrue(firstTemplate.getMessage().contains("Reply Next to navigate"));

        Hook navigationHook = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .additionalData(Map.of("dynamicChoice", Map.of(
                        "id", "availableDrivers::NEXT::1",
                        "label", "Next",
                        "metadata", Map.of("_jawcePagination", Map.of(
                                "action", "NEXT",
                                "stateKey", DriverSelectionHook.PAGINATION_STATE_KEY,
                                "targetPage", 1
                        ))
                )))
                .build();

        hook.capture(navigationHook);
        Hook routed = hook.route(navigationHook);
        assertEquals(DriverSelectionHook.RERENDER_STAGE, routed.getRedirectTo());

        Hook secondPage = hook.render(Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .build());
        TextTemplate secondTemplate = (TextTemplate) secondPage.getTemplateDynamicBody().getTemplate();
        assertTrue(secondTemplate.getMessage().contains("Page 2 of 2"));
        assertTrue(secondTemplate.getMessage().contains("1. C. Mlambo"));
        assertTrue(secondTemplate.getMessage().contains("Reply Back to navigate"));

        Hook selectionHook = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .additionalData(Map.of("dynamicChoice", Map.of(
                        "id", "drv-311",
                        "label", "C. Mlambo",
                        "ordinal", 1,
                        "metadata", Map.of(
                                "driverId", "drv-311",
                                "driverName", "C. Mlambo",
                                "vehicle", "BMW 520d",
                                "plate", "LUX 311",
                                "etaMins", 7,
                                "priceNote", "Premium meet-and-greet"
                        )
                )))
                .build();

        hook.capture(selectionHook);

        assertEquals("C. Mlambo", sessionManager.get(sessionId, "selectedDriverName", String.class));
        assertEquals("BMW 520d", sessionManager.get(sessionId, "selectedDriverVehicle", String.class));
        assertEquals("LUX 311", sessionManager.get(sessionId, "selectedDriverPlate", String.class));
        assertEquals(7, sessionManager.get(sessionId, "selectedDriverEtaMins", Integer.class));
        assertEquals("Premium meet-and-greet", sessionManager.get(sessionId, "selectedDriverPriceNote", String.class));
        assertNull(PaginationSupport.state(selectionHook, DriverSelectionHook.PAGINATION_STATE_KEY));
    }
}
