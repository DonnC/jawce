package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.core.HookArgRest;
import zw.co.dcl.jawce.engine.processor.iface.IHookService;
import zw.co.dcl.jawce.engine.api.Worker;
import zw.co.dcl.jawce.session.ISessionManager;

public abstract class AbstractHookService extends BaseHook implements IHookService {
    protected Worker engine;

    @Override
    public void setup(HookArgRest args, ISessionManager sessionManager, Worker engineService) {
        initialize(args, sessionManager);
        this.engine = engineService;
    }
}
