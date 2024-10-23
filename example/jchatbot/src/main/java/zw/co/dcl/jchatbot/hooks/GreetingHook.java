package zw.co.dcl.jchatbot.hooks;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType;
import zw.co.dcl.jawce.engine.model.dto.HookArgs;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jchatbot.configs.SessionLocator;

import java.util.Map;

@Slf4j
public class GreetingHook {
    private final HookArgs args;
    private final ISessionManager session;
    private final String sessionId;


    public GreetingHook(HookArgs args) {
        this.args = args;
        this.sessionId = args.getChannelUser().waId();
        this.session = SessionLocator.getSessionManager().session(sessionId);
    }

    public Object getDefaultUsername() {

        args.setTemplateDynamicBody(
                new TemplateDynamicBody(
                        WebhookResponseMessageType.BUTTON,
                        null,
                        Map.of("user", args.getChannelUser().name())
                )
        );

        return args;
    }
}
