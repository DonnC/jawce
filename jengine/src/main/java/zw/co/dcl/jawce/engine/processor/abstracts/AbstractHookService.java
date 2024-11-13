package zw.co.dcl.jawce.engine.processor.abstracts;

import zw.co.dcl.jawce.engine.model.dto.HookArgsRest;
import zw.co.dcl.jawce.engine.processor.iface.IHookService;
import zw.co.dcl.jawce.engine.service.EntryService;
import zw.co.dcl.jawce.session.ISessionManager;

public abstract class AbstractHookService extends BaseHook implements IHookService {
    protected EntryService engine;

    @Override
    public void setup(HookArgsRest args, ISessionManager sessionManager, EntryService engineService) {
        initialize(args, sessionManager);
        this.engine = engineService;
    }
}
