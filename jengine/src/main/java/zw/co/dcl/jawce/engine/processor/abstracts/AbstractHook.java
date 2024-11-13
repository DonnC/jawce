package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.dto.HookArgs;
import zw.co.dcl.jawce.session.ISessionManager;

import java.util.Map;

public abstract class AbstractHook extends BaseHook {
    protected AbstractHook(HookArgs args, ISessionManager sessionManager) {
        initialize(args, sessionManager);
    }
}
