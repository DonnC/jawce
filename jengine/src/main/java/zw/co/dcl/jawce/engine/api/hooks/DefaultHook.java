package zw.co.dcl.jawce.engine.api.hooks;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.Map;

@Slf4j
public class DefaultHook {
    /**
     * Gets the default WhatsApp user and also set it in session
     * <p>
     * set name can be accessed via shorthand as {{ s.whatsappName }} in template
     *
     * @param arg: Hook passed by the engine
     * @return updated Hook
     */
    public Hook username(Hook arg) {
        log.debug("[getUsername] args: {}", arg);

        arg.getSession().save(arg.getSessionId(), "whatsappName", arg.getWaUser().name());

        arg.setTemplateDynamicBody(
                new TemplateDynamicBody(
                        null,
                        null,
                        Map.of("whatsappName", arg.getWaUser().name())
                )
        );

        return arg;
    }
}
