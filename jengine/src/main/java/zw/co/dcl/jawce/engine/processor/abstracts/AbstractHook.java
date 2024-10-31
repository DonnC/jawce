package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.dto.HookArgs;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public abstract class AbstractHook {
    protected final HookArgs args;
    protected final ISessionManager session;
    protected final String sessionId;
    protected final Map<String, Object> props;

    protected AbstractHook(HookArgs args, ISessionManager sessionManager) {
        this.args = args;
        this.sessionId = args.getChannelUser().waId();
        this.session = sessionManager.session(sessionId);
        this.props = this.session.getUserProps(this.sessionId);
    }
}
