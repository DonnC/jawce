package zw.co.dcl.jchatbot.hooks;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;
import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.processor.abstracts.AbstractHook;
import zw.co.dcl.jchatbot.configs.SessionLocator;

import java.util.Map;

@Slf4j
public class GreetingHook extends AbstractHook {
    public GreetingHook(HookArg args) {
        super(args, SessionLocator.getSessionManager());
    }

    public Object getDefaultUsername() {
        log.info("[getDefaultUsername] args: {}", this.args);

        args.setTemplateDynamicBody(
                new TemplateDynamicBody(
                        WebhookResponseMessageType.BUTTON,
                        null,
                        Map.of("user", args.getWaUser().name())
                )
        );

        return args;
    }
}
