package zw.co.dcl.ehailing.hooks;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CaptureHook {
    /**
     * Simulate capturing user ride request
     *
     * @param arg: Hook passed by the engine
     * @return updated Hook
     */
    public Hook capture(Hook arg) {
        log.debug("[capture] Received hook arg: {}", arg);
        log.debug("[capture] User selected the: `{}` option", arg.getUserInput());

        // simulate request processing (10 seconds)
        try {
            var userProps = arg.getSession().getUserProps(arg.getSessionId());

            log.info("[capture] Current user props: {}", userProps);

            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("[capture] Sleep interrupted while processing capture hook");
        }

        return arg;
    }
}
