package zw.co.dcl.jawce.engine.model.dto;

import java.util.Map;

public record WebhookProcessorResult(
        Map<String, Object> payload,
        String nextRoute,
        String sessionId,
        boolean handleSession
) {
}
