package zw.co.dcl.jawce.engine.internal.dto;

import org.junit.jupiter.api.Test;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.model.template.TextTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HookResultMapperTest {
    @Test
    void mapsGenerateHookTemplateOverrideAndPayloads() {
        TextTemplate template = new TextTemplate();
        template.setType("text");
        template.setMessage("Hello");

        Hook hook = Hook.builder()
                .templateDynamicBody(
                        TemplateDynamicBody.builder()
                                .template(template)
                                .renderPayload(Map.of("name", "Donald"))
                                .flowPayload(Map.of("screen", "START"))
                                .build()
                )
                .build();

        GenerateHookResult result = HookResultMapper.toGenerateResult(hook);

        assertSame(hook, result.hook());
        assertSame(template, result.templateOverride());
        assertEquals("Donald", result.renderPayload().get("name"));
        assertEquals("START", result.flowPayload().get("screen"));
        assertTrue(result.hasTemplateOverride());
        assertTrue(result.hasRenderPayload());
        assertTrue(result.hasFlowPayload());
    }

    @Test
    void mapsEmptyGenerateHookResultSafely() {
        Hook hook = Hook.builder().build();

        GenerateHookResult result = HookResultMapper.toGenerateResult(hook);

        assertSame(hook, result.hook());
        assertNull(result.templateOverride());
        assertFalse(result.hasTemplateOverride());
        assertFalse(result.hasRenderPayload());
        assertFalse(result.hasFlowPayload());
    }

    @Test
    void mergeRetainsExistingContextAndAddsNewValues() {
        Hook fallback = Hook.builder()
                .sessionId("263771234567")
                .redirectTo("REPORT")
                .params(Map.of("existing", "value"))
                .build();
        Hook update = Hook.builder()
                .params(Map.of("fresh", "value"))
                .build();

        Hook merged = HookResultMapper.merge(update, fallback);

        assertEquals("263771234567", merged.getSessionId());
        assertEquals("REPORT", HookResultMapper.redirectTo(merged));
        assertEquals("value", merged.getParams().get("existing"));
        assertEquals("value", merged.getParams().get("fresh"));
        assertNull(HookResultMapper.redirectTo(null));
    }
}
