package zw.co.dcl.jchatbot;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class WebhookEvent extends ApplicationEvent {
    private final Map<String, Object> payload;
    private final Map<String, Object> headers;

    public WebhookEvent(Object source, Map<String, Object> payload, Map<String, Object> headers) {
        super(source);
        this.payload = payload;
        this.headers = headers;
    }
}
