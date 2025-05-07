package zw.co.dcl.jawce.engine.internal.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import zw.co.dcl.jawce.engine.model.core.Hook;

@Getter
public class OnceOffHookEvent extends ApplicationEvent {
    private final Hook arg;

    public OnceOffHookEvent(Object source, Hook hookArg) {
        super(source);
        this.arg = hookArg;
    }
}
