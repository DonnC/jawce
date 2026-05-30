package zw.co.dcl.jawce.engine.internal.dto;

import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.Hook;

import java.util.Collections;
import java.util.Map;

public record GenerateHookResult(
        Hook hook,
        BaseEngineTemplate templateOverride,
        Map<String, Object> renderPayload,
        Map<String, Object> flowPayload
) {
    public GenerateHookResult {
        renderPayload = renderPayload == null ? Collections.emptyMap() : Collections.unmodifiableMap(renderPayload);
        flowPayload = flowPayload == null ? Collections.emptyMap() : Collections.unmodifiableMap(flowPayload);
    }

    public boolean hasTemplateOverride() {
        return this.templateOverride != null;
    }

    public boolean hasRenderPayload() {
        return !this.renderPayload.isEmpty();
    }

    public boolean hasFlowPayload() {
        return !this.flowPayload.isEmpty();
    }
}
