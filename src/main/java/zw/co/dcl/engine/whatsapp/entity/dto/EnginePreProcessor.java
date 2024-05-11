package zw.co.dcl.engine.whatsapp.entity.dto;

import java.util.Map;

public record EnginePreProcessor(
        String stage,
        Map<String, Object> template
) {
}
