package zw.co.dcl.jawce.engine.model.dto;

import java.util.Map;

public record MsgProcessorResponseDTO(
        Map<String, Object> payload,
        String nextRoute,
        String recipient
) {
}
