package zw.co.dcl.jawce.engine.api.iface.hook;

import zw.co.dcl.jawce.engine.model.core.Hook;

public interface IDynamicHook {
    Hook execute(Hook hook) throws Exception;
}
