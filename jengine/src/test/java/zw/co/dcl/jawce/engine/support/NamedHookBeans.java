package zw.co.dcl.jawce.engine.support;

import zw.co.dcl.jawce.engine.api.annotation.FlowHookType;
import zw.co.dcl.jawce.engine.api.annotation.NamedFlowHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IGenerateHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IRouterHook;
import zw.co.dcl.jawce.engine.api.iface.hook.ITemplateHook;
import zw.co.dcl.jawce.engine.api.iface.hook.IReceiveHook;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class NamedHookBeans {
    private NamedHookBeans() {
    }

    @NamedFlowHook("namedReceive")
    public static class NamedReceiveHook implements IReceiveHook {
        @Override
        public Hook execute(Hook hook) {
            List<String> events = hook.getSession().getGlobal("events", List.class);
            List<String> nextEvents = events == null ? new ArrayList<>() : new ArrayList<>(events);
            nextEvents.add("named_receive");
            hook.getSession().saveGlobal("events", nextEvents);
            return hook;
        }
    }

    @NamedFlowHook("namedGenerate")
    public static class NamedGenerateHook implements IGenerateHook {
        @Override
        public Hook execute(Hook hook) {
            hook.setTemplateDynamicBody(
                    TemplateDynamicBody.builder()
                            .renderPayload(new HashMap<>(java.util.Map.of("name", "Named")))
                            .build()
            );
            return hook;
        }
    }

    @NamedFlowHook("namedRouter")
    public static class NamedRouterHook implements IRouterHook {
        @Override
        public Hook execute(Hook hook) {
            hook.setRedirectTo("REPORT");
            return hook;
        }
    }

    @NamedFlowHook("namedTemplate")
    public static class NamedTemplateHook implements ITemplateHook {
        @Override
        public Hook execute(Hook hook) {
            hook.setTemplateDynamicBody(
                    TemplateDynamicBody.builder()
                            .renderPayload(new HashMap<>(java.util.Map.of("name", "NamedTemplate")))
                            .build()
            );
            return hook;
        }
    }

    public static class NamedMethodHooks {
        @NamedFlowHook(value = "methodReceive", type = FlowHookType.RECEIVE)
        public Hook receive(Hook hook) {
            List<String> events = hook.getSession().getGlobal("events", List.class);
            List<String> nextEvents = events == null ? new ArrayList<>() : new ArrayList<>(events);
            nextEvents.add("method_receive");
            hook.getSession().saveGlobal("events", nextEvents);
            return hook;
        }

        @NamedFlowHook(value = "methodGenerate", type = FlowHookType.GENERATE)
        public Hook generate(Hook hook) {
            hook.setTemplateDynamicBody(
                    TemplateDynamicBody.builder()
                            .renderPayload(new HashMap<>(java.util.Map.of("name", "MethodNamed")))
                            .build()
            );
            return hook;
        }

        @NamedFlowHook(value = "methodRouter", type = FlowHookType.ROUTER)
        public Hook route(Hook hook) {
            hook.setRedirectTo("REPORT");
            return hook;
        }
    }
}
