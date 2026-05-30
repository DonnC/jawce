package zw.co.dcl.jawce.engine.internal.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import zw.co.dcl.jawce.engine.configs.JawceConfig;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.support.EngineTestSupport;
import zw.co.dcl.jawce.engine.support.TestHooks;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HookServiceCachingTest {
    @Test
    void cachesReflectivePlanForManualHooks() throws Exception {
        HookService hookService = new HookService(
                new EngineTestSupport.RecordingClientManager(),
                new JawceConfig(),
                new StaticApplicationContext()
        );

        Hook hook = new Hook();
        hook.setHook(TestHooks.class.getName() + ".routeToReport");

        hookService.processHook(hook);
        hookService.processHook(hook);

        assertEquals(1, hookService.reflectiveHookPlanCacheSize());
    }

    @Test
    void cachesReflectivePlanForSpringManagedHooks() throws Exception {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.registerSingleton(SpringManagedHook.class.getName(), SpringManagedHook.class);
        applicationContext.refresh();

        HookService hookService = new HookService(
                new EngineTestSupport.RecordingClientManager(),
                new JawceConfig(),
                applicationContext
        );

        Hook hook = new Hook();
        hook.setHook(SpringManagedHook.class.getName() + ".redirect");

        hookService.processHook(hook);
        hookService.processHook(hook);

        assertEquals(1, hookService.reflectiveHookPlanCacheSize());
    }

    public static class SpringManagedHook {
        public Hook redirect(Hook hook) {
            hook.setRedirectTo("REPORT");
            return hook;
        }
    }
}
