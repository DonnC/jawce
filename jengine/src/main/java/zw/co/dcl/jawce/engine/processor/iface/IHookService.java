package zw.co.dcl.jawce.engine.processor.iface;

import zw.co.dcl.jawce.engine.model.dto.HookArgsRest;
import zw.co.dcl.jawce.engine.service.EntryService;
import zw.co.dcl.jawce.session.ISessionManager;

public interface IHookService {
    void setup(HookArgsRest args, ISessionManager sessionManager, EntryService engineService);
}
