package zw.co.dcl.jawce.engine.internal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class WebhookEvent extends ApplicationEvent {
    private final Map<String, Object> payload;

    public WebhookEvent(Object source, Map<String, Object> payload) {
        super(source);
        this.payload = payload;
    }
}
