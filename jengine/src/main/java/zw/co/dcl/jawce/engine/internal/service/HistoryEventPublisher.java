package zw.co.dcl.jawce.engine.internal.service;

import org.springframework.context.ApplicationEventPublisher;
import zw.co.dcl.jawce.engine.model.history.ChatHistoryEvent;

public class HistoryEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public HistoryEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publish(ChatHistoryEvent event) {
        this.applicationEventPublisher.publishEvent(event);
    }
}
