package zw.co.dcl.jawce.engine.internal.service;

import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.engine.api.iface.IHistoryManager;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;
import zw.co.dcl.jawce.engine.model.history.HistoryEventType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoryEventListenerTest {
    @Test
    void listenerDispatchesEventsToHistoryManager() throws Exception {
        RecordingHistoryManager historyManager = new RecordingHistoryManager();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        HistoryEventListener listener = new HistoryEventListener(historyManager, executorService);

        listener.handle(ChatHistoryEvent.builder()
                .timestamp("2026-05-30T10:15:30")
                .type(HistoryEventType.OUTBOUND_SENT)
                .direction("outbound")
                .sessionId("263771234567")
                .build());

        executorService.shutdown();
        executorService.awaitTermination(2, TimeUnit.SECONDS);

        assertEquals(1, historyManager.events.size());
        assertEquals(HistoryEventType.OUTBOUND_SENT, historyManager.events.get(0).getType());
    }

    static class RecordingHistoryManager implements IHistoryManager {
        private final List<ChatHistoryEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void record(ChatHistoryEvent event) {
            events.add(event);
        }
    }
}
