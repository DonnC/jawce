package zw.co.dcl.engine.whatsapp.entity.dto;

import java.util.Map;

public record MsgProcessorResponseDTO(
        Map<String, Object> payload,
        String nextRoute,
        String recipient
) {
}
