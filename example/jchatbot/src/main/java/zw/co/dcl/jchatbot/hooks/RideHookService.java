package zw.co.dcl.jchatbot.hooks;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import zw.co.dcl.jawce.engine.model.dto.HookArgsRest;
import zw.co.dcl.jawce.engine.processor.abstracts.AbstractHookService;
import zw.co.dcl.jawce.session.ISessionManager;
import zw.co.dcl.jchatbot.service.WebhookConfigService;

@Slf4j
@Service
public class RideHookService extends AbstractHookService {
    @Autowired
    private WebhookConfigService configService;
    @Autowired
    private ISessionManager sessionManager;

    public RideHookService setup(HookArgsRest args) {
        super.setup(args, sessionManager, configService.getEntryInstance());
        return this;
    }

    public Object processRide() {
        // get data from session
        var rideType = props.get("RideType");

        // process ride logic

        return args;
    }
}
