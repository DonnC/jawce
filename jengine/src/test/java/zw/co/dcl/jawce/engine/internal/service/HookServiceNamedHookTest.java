package zw.co.dcl.jawce.engine.internal.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;
import zw.co.dcl.jawce.engine.support.NamedHookBeans;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HookServiceNamedHookTest {
    @Test
    void namedReceiveHookResolvesFromRegistry() throws Exception {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton("namedReceiveHook", NamedHookBeans.NamedReceiveHook.class);
        applicationContext.refresh();

        HookService hookService = new HookService(
                new EngineTestSupport.RecordingClientManager(),
                new JawceConfig(),
                applicationContext,
                new FlowHookRegistry(applicationContext)
        );

        EngineTestSupport.InMemorySessionManager sessionManager = new EngineTestSupport.InMemorySessionManager();
        Hook hook = Hook.builder()
                .hook("namedReceive")
                .session(sessionManager)
                .sessionId("263771234567")
                .build();

        hookService.processHook(hook, HookExecutionType.RECEIVE);

        assertEquals(java.util.List.of("named_receive"), sessionManager.getGlobal("events", java.util.List.class));
    }

    @Test
    void namedRouterHookResolvesFromRegistry() throws Exception {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton("namedRouterHook", NamedHookBeans.NamedRouterHook.class);
        applicationContext.refresh();

        HookService hookService = new HookService(
                new EngineTestSupport.RecordingClientManager(),
                new JawceConfig(),
                applicationContext,
                new FlowHookRegistry(applicationContext)
        );

        Hook hook = Hook.builder()
                .hook("namedRouter")
                .build();

        Hook result = hookService.processHook(hook, HookExecutionType.ROUTER);

        assertEquals("REPORT", result.getRedirectTo());
    }

    @Test
    void namedMethodReceiveHookResolvesFromRegistry() throws Exception {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton("namedMethodHooks", NamedHookBeans.NamedMethodHooks.class);
        applicationContext.refresh();

        HookService hookService = new HookService(
                new EngineTestSupport.RecordingClientManager(),
                new JawceConfig(),
                applicationContext,
                new FlowHookRegistry(applicationContext)
        );

        EngineTestSupport.InMemorySessionManager sessionManager = new EngineTestSupport.InMemorySessionManager();
        Hook hook = Hook.builder()
                .hook("methodReceive")
                .session(sessionManager)
                .sessionId("263771234567")
                .build();

        hookService.processHook(hook, HookExecutionType.RECEIVE);

        assertEquals(java.util.List.of("method_receive"), sessionManager.getGlobal("events", java.util.List.class));
    }

    @Test
    void namedMethodRouterHookResolvesFromRegistry() throws Exception {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton("namedMethodHooks", NamedHookBeans.NamedMethodHooks.class);
        applicationContext.refresh();

        HookService hookService = new HookService(
                new EngineTestSupport.RecordingClientManager(),
                new JawceConfig(),
                applicationContext,
                new FlowHookRegistry(applicationContext)
        );

        Hook hook = Hook.builder()
                .hook("methodRouter")
                .build();

        Hook result = hookService.processHook(hook, HookExecutionType.ROUTER);

        assertEquals("REPORT", result.getRedirectTo());
    }
}
