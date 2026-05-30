package zw.co.dcl.jawce.engine.internal.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import zw.co.dcl.jawce.engine.api.iface.IHistoryManager;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;

import java.util.concurrent.ExecutorService;

@Slf4j
public class HistoryEventListener {
    private final IHistoryManager historyManager;
    private final ExecutorService executorService;

    public HistoryEventListener(IHistoryManager historyManager, ExecutorService executorService) {
        this.historyManager = historyManager;
        this.executorService = executorService;
    }

    @EventListener
    public void handle(ChatHistoryEvent event) {
        this.executorService.submit(() -> {
            try {
                this.historyManager.record(event);
            } catch (Exception e) {
                log.warn("Failed to record history event: {}", e.getMessage());
            }
        });
    }
}
