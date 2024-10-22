package zw.co.dcl.jawce.engine.model.dto;

import java.util.Map;

public record EnginePreProcessor(
        String stage,
        Map<String, Object> template
) {
}
