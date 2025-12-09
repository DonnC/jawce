package zw.co.dcl.jawce.engine.internal.dto;

import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;

public record PreProcessorResult(
        String stage,
        BaseEngineTemplate template
) {
}
