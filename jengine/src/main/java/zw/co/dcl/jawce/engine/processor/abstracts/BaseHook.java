package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.abs.AbsHookArg;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public abstract class BaseHook {
    protected AbsHookArg args;
    protected ISessionManager session;
    protected String sessionId;
    protected Map<String, Object> props;
    protected Map<String, Object> additionalData;
    protected Map<String, Object> params;

    protected void initialize(AbsHookArg args, ISessionManager sessionManager) {
        this.args = args;
        this.sessionId = args.getWaUser().waId();
        this.session = sessionManager.session(sessionId);
        this.props = this.session.getUserProps(this.sessionId);
        this.additionalData = args.getAdditionalData();
        this.params = args.getParams();
    }
}
