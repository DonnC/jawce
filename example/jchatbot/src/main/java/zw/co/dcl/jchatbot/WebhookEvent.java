package zw.co.dcl.jchatbot;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class WebhookEvent extends ApplicationEvent {
    private final Map<String, Object> payload;
    private final HttpServletRequest request;

    public WebhookEvent(Object source, Map<String, Object> payload, HttpServletRequest request) {
        super(source);
        this.payload = payload;
        this.request = request;
    }
}
