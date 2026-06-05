package zw.co.dcl.jawce.engine.internal.service;

import zw.co.dcl.jawce.engine.api.annotation.FlowHookType;

public enum HookExecutionType {
    RECEIVE,
    GENERATE,
    DYNAMIC,
    ROUTER,
    MIDDLEWARE,
    TEMPLATE,
    GENERIC;

    public static HookExecutionType from(FlowHookType type) {
        return HookExecutionType.valueOf(type.name());
    }
}
