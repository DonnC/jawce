package zw.co.dcl.jawce.engine.processor.iface;

import zw.co.dcl.jawce.engine.model.core.HookArgRest;
import zw.co.dcl.jawce.engine.JawceEngineWorker;
import zw.co.dcl.jawce.session.ISessionManager;

public interface IHookService {
    void setup(HookArgRest args, ISessionManager sessionManager, JawceEngineWorker engineService);
}
