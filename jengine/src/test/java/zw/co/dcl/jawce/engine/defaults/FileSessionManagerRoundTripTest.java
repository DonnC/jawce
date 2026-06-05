package zw.co.dcl.jawce.engine.defaults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zw.co.dcl.jawce.engine.configs.FileSessionProperties;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.state.ConversationState;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSessionManagerRoundTripTest {
    @TempDir
    Path tempDir;

    private FileSessionManager createManager() {
        FileSessionProperties properties = new FileSessionProperties();
        properties.setDir(tempDir.resolve("sessions").toString());
        return new FileSessionManager(properties);
    }

    @Test
    void persistsDynamicChoicesWithConcreteTypesAcrossRequests() {
        FileSessionManager manager = createManager();
        String sessionId = "263771234567";

        manager.save(
                sessionId,
                SessionConstant.SESSION_DYNAMIC_CHOICE_REGISTRY_KEY,
                List.of(DynamicChoice.builder()
                        .id("acc-1")
                        .label("Savings")
                        .ordinal(1)
                        .aliases(List.of("savings"))
                        .build())
        );

        ConversationState state = new ConversationState(sessionId, manager);
        List<DynamicChoice> choices = state.dynamicChoices();

        assertEquals(1, choices.size());
        assertEquals("acc-1", choices.get(0).getId());
        assertEquals("Savings", choices.get(0).getLabel());
        assertEquals(1, choices.get(0).getOrdinal());
    }

    @Test
    void persistsMessageHistoryWithoutLosingOrderedSetBehavior() {
        FileSessionManager manager = createManager();
        String sessionId = "263771234568";

        manager.save(
                sessionId,
                SessionConstant.SESSION_MESSAGE_HISTORY_KEY,
                new LinkedHashSet<>(List.of("wamid-1", "wamid-2"))
        );

        ConversationState state = new ConversationState(sessionId, manager);

        assertTrue(state.hasProcessedMessageId("wamid-1"));
        assertEquals(new LinkedHashSet<>(List.of("wamid-1", "wamid-2")), state.messageHistory());
    }
}
