package zw.co.dcl.ehailing.hooks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zw.co.dcl.jawce.engine.configs.FileSessionProperties;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.template.ButtonTemplate;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class RideOfferHookTest {
    @TempDir
    Path tempDir;

    private FileSessionManager createSessionManager() {
        FileSessionProperties properties = new FileSessionProperties();
        properties.setDir(tempDir.resolve("sessions").toString());
        return new FileSessionManager(properties);
    }

    @Test
    void prepareAndRenderUsesStandardQuote() {
        FileSessionManager sessionManager = createSessionManager();
        String sessionId = "263771234567";
        sessionManager.saveProp(sessionId, "rideType", "Standard");
        sessionManager.save(sessionId, "selectedDriverName", "Nyasha G.");
        sessionManager.save(sessionId, "selectedDriverEtaMins", 8);
        sessionManager.save(sessionId, "selectedDriverPriceNote", "Economy fare");

        RideOfferHook hook = new RideOfferHook();
        Hook arg = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .build();

        hook.prepare(arg);
        Hook result = hook.render(arg);

        assertEquals("4.25", sessionManager.get(sessionId, "quoteAmount", String.class));
        assertEquals(6, sessionManager.get(sessionId, "quoteWaitMins", Integer.class));
        assertInstanceOf(ButtonTemplate.class, result.getTemplateDynamicBody().getTemplate());

        ButtonTemplate template = (ButtonTemplate) result.getTemplateDynamicBody().getTemplate();
        assertEquals("Your Standard fee to your destination is USD $4.25\nYou will arrive in ~6mins", template.getMessage().getBody());
        assertEquals("Driver Nyasha G. | 8 mins away | Economy fare", template.getMessage().getFooter());
        assertEquals(java.util.List.of("Accept", "Counter Offer"), template.getMessage().getButtons());
    }

    @Test
    void prepareAndRenderFallsBackToBaseRideQuote() {
        FileSessionManager sessionManager = createSessionManager();
        String sessionId = "263771234568";

        RideOfferHook hook = new RideOfferHook();
        Hook arg = Hook.builder()
                .session(sessionManager)
                .sessionId(sessionId)
                .build();

        hook.prepare(arg);
        Hook result = hook.render(arg);

        assertEquals("3.50", sessionManager.get(sessionId, "quoteAmount", String.class));
        assertEquals(8, sessionManager.get(sessionId, "quoteWaitMins", Integer.class));

        ButtonTemplate template = (ButtonTemplate) result.getTemplateDynamicBody().getTemplate();
        assertEquals("Your Ride fee to your destination is USD $3.50\nYou will arrive in ~8mins", template.getMessage().getBody());
        assertEquals("Driver is being matched | 8 mins away | Metered fare", template.getMessage().getFooter());
    }
}
