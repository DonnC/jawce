package zw.co.dcl.jawce.engine.internal.service;

import zw.co.dcl.jawce.engine.model.core.Hook;

@FunctionalInterface
public interface HookInvocationPlan {
    Hook invoke(Hook hook) throws Exception;
}
