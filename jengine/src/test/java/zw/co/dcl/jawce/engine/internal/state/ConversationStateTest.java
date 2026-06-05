package zw.co.dcl.jawce.engine.internal.state;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.engine.constants.SessionConstant;
import zw.co.dcl.jawce.engine.internal.recovery.RecoveryIntent;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversationStateTest {
    private EngineTestSupport.InMemorySessionManager sessionManager;
    private ConversationState conversationState;

    @BeforeEach
    void setUp() {
        this.sessionManager = new EngineTestSupport.InMemorySessionManager();
        this.conversationState = new ConversationState("263771234567", sessionManager);
    }

    @Test
    void initializeAtStartMenuSeedsCurrentAndPreviousStage() {
        conversationState.initializeAtStartMenu("START-MENU");

        assertEquals("START-MENU", conversationState.currentStage());
        assertEquals("START-MENU", conversationState.previousStage());
    }

    @Test
    void advanceToMovesCurrentStageToPreviousStage() {
        conversationState.initializeAtStartMenu("START-MENU");

        conversationState.advanceTo("REPORT");

        assertEquals("START-MENU", conversationState.previousStage());
        assertEquals("REPORT", conversationState.currentStage());
    }

    @Test
    void rollbackOrClearRollsBackForMidFlowErrors() {
        conversationState.saveAll(Map.of(
                SessionConstant.PREV_STAGE, "START-MENU",
                SessionConstant.CURRENT_STAGE, "REPORT"
        ));

        conversationState.rollbackOrClear("START-MENU");

        assertEquals("START-MENU", conversationState.currentStage());
    }

    @Test
    void rollbackOrClearClearsAtStartMenu() {
        conversationState.saveAll(Map.of(
                SessionConstant.PREV_STAGE, "START-MENU",
                SessionConstant.CURRENT_STAGE, "START-MENU",
                SessionConstant.AUTH_SET_KEY, "yes"
        ));

        conversationState.rollbackOrClear("START-MENU");

        assertNull(conversationState.currentStage());
        assertNull(conversationState.get(SessionConstant.AUTH_SET_KEY));
    }

    @Test
    void markRetryPendingStoresCheckpointFromCurrentStage() {
        conversationState.setCurrentStage("REPORT");

        conversationState.markRetryPending();

        assertTrue(conversationState.isRetryPending());
        assertEquals("REPORT", conversationState.checkpointStage());
    }

    @Test
    void rememberMessageIdTracksDuplicateProtectionQueue() {
        conversationState.rememberMessageId("wamid-1");

        assertTrue(conversationState.hasProcessedMessageId("wamid-1"));
        assertEquals(1, conversationState.messageHistory().size());
    }

    @Test
    void rememberMessageIdTrimsQueueWhenLimitIsExceeded() {
        IntStream.rangeClosed(1, 15)
                .forEach(i -> conversationState.rememberMessageId("wamid-" + i));

        assertTrue(conversationState.messageHistory().size() <= 10);
        assertEquals(Set.of(
                "wamid-6", "wamid-7", "wamid-8", "wamid-9", "wamid-10",
                "wamid-11", "wamid-12", "wamid-13", "wamid-14", "wamid-15"
        ), conversationState.messageHistory());
    }

    @Test
    void debounceTimestampIsStoredAndReadable() {
        conversationState.markDebounceAt(12345L);

        assertEquals(12345L, conversationState.currentDebounceTimestamp());
    }

    @Test
    void messageHistoryReadsLegacyListFormatWithoutCrashing() {
        conversationState.save(SessionConstant.SESSION_MESSAGE_HISTORY_KEY, java.util.List.of("wamid-1", "wamid-2"));

        assertTrue(conversationState.hasProcessedMessageId("wamid-1"));
        assertEquals(Set.of("wamid-1", "wamid-2"), conversationState.messageHistory());
    }

    @Test
    void dynamicChoicesReadLegacyMapListFormatWithoutCrashing() {
        conversationState.save(
                SessionConstant.SESSION_DYNAMIC_CHOICE_REGISTRY_KEY,
                List.of(Map.of(
                        "id", "acc-1",
                        "label", "Savings",
                        "ordinal", 1,
                        "aliases", List.of("savings")
                ))
        );

        List<DynamicChoice> choices = conversationState.dynamicChoices();

        assertEquals(1, choices.size());
        assertEquals("acc-1", choices.get(0).getId());
        assertEquals("Savings", choices.get(0).getLabel());
        assertEquals(1, choices.get(0).getOrdinal());
    }

    @Test
    void recoveryActionsRoundTripAndMatchCaseInsensitively() {
        Map<String, RecoveryIntent> actions = new LinkedHashMap<>();
        actions.put("Menu", RecoveryIntent.RETURN_TO_MENU);

        conversationState.rememberRecoveryActions(actions);

        assertEquals(RecoveryIntent.RETURN_TO_MENU, conversationState.recoveryIntentFor("menu").orElseThrow());
        assertEquals(RecoveryIntent.RETURN_TO_MENU, conversationState.get(SessionConstant.SESSION_RECOVERY_ACTIONS_KEY, Map.class).get("Menu"));
    }
}
