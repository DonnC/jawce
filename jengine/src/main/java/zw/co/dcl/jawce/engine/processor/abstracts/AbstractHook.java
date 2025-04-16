package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.session.ISessionManager;

public abstract class AbstractHook extends BaseHook {
    protected AbstractHook(HookArg args, ISessionManager sessionManager) {
        initialize(args, sessionManager);
    }
}
