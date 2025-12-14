package zw.co.dcl.jawce.engine.internal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.WaUser;

/**
 * Event class used for handling once-off messaging events within the application.
 * This class inherits from {@link ApplicationEvent}
 * and is designed to carry an {@link BaseEngineTemplate}
 * object as its payload.
 * <p>
 * The {@link BaseEngineTemplate} represents the template
 * data used for constructing the message associated with this event. It acts as a base
 * class for a variety of concrete implementations, such as text, button, flow, or media templates.
 * <p>
 * This event is typically triggered when a single messaging action needs to be processed
 * using specific template and routing data.
 * <p>
 * Constructor Parameters:
 * - source: The object on which the event initially occurred or originated.
 * - template: An instance of {@link BaseEngineTemplate}
 * that holds the template data associated with the event.
 */
@Getter
public class OnceOffMessageEvent extends ApplicationEvent {
    private final BaseEngineTemplate template;
    private final WaUser user;

    public OnceOffMessageEvent(Object source, WaUser user, BaseEngineTemplate template) {
        super(source);
        this.user = user;
        this.template = template;
    }
}
