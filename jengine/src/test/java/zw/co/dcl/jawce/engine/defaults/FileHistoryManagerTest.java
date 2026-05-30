package zw.co.dcl.jawce.engine.defaults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import zw.co.dcl.jawce.engine.configs.HistoryProperties;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;
import zw.co.dcl.jawce.engine.model.history.HistoryEventType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHistoryManagerTest {
    @TempDir
    Path tempDir;

    @Test
    void recordsEventsAsNdjsonLines() throws Exception {
        HistoryProperties properties = new HistoryProperties();
        properties.setDir(tempDir.toString());
        properties.setFileEnabled(true);
        FileHistoryManager manager = new FileHistoryManager(properties);

        manager.record(sampleEvent("1"));
        manager.record(sampleEvent("2"));

        Path historyFile = Files.list(tempDir).findFirst().orElseThrow();
        List<String> lines = Files.readAllLines(historyFile);

        assertEquals(2, lines.size());
        assertTrue(lines.get(0).contains("\"type\":\"INBOUND_RECEIVED\""));
        assertTrue(lines.get(1).contains("\"messageId\":\"2\""));
    }

    @Test
    void rotatesFilesAndPrunesOldOnesWhenSizeLimitIsReached() throws Exception {
        HistoryProperties properties = new HistoryProperties();
        properties.setDir(tempDir.toString());
        properties.setFileEnabled(true);
        properties.setMaxFileSizeBytes(220);
        properties.setMaxFiles(2);
        FileHistoryManager manager = new FileHistoryManager(properties);

        for(int i = 0; i < 6; i++) {
            manager.record(sampleEvent("msg-" + i));
        }

        List<Path> files = Files.list(tempDir).sorted().toList();

        assertEquals(2, files.size());
        for(Path file : files) {
            assertTrue(Files.size(file) <= 220L + 220L, "history file should remain bounded after rotation");
        }
    }

    private ChatHistoryEvent sampleEvent(String messageId) {
        return ChatHistoryEvent.builder()
                .timestamp("2026-05-30T10:15:30")
                .type(HistoryEventType.INBOUND_RECEIVED)
                .direction("inbound")
                .sessionId("263771234567")
                .waId("263771234567")
                .messageId(messageId)
                .stage("START-MENU")
                .detail("accepted")
                .payload(java.util.Map.of("text", "hello-" + messageId))
                .build();
    }
}
