package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.dto.HookArgsRest;
import zw.co.dcl.jawce.engine.processor.iface.IHookService;
import zw.co.dcl.jawce.engine.service.EntryService;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public abstract class AbstractHookService implements IHookService {
    protected HookArgsRest args;
    protected ISessionManager session;
    protected String sessionId;
    protected Map<String, Object> props;
    protected EntryService engine;

    @Override
    public void setup(HookArgsRest args, ISessionManager sessionManager, EntryService engineService) {
        this.args = args;
        this.sessionId = args.getChannelUser().waId();
        this.session = sessionManager.session(sessionId);
        this.props = this.session.getUserProps(this.sessionId);
        this.engine = engineService;
    }
}
