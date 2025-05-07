package zw.co.dcl.jchatbot.hooks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.Map;

@Slf4j
@Service
public class GreetingHook {
    public Hook getDefaultUsername(Hook arg) {
        log.info("[getDefaultUsername] args: {}", arg);

        arg.setTemplateDynamicBody(
                new TemplateDynamicBody(
                        null,
                        null,
                        Map.of("user", arg.getWaUser().name())
                )
        );

        return arg;
    }
}
