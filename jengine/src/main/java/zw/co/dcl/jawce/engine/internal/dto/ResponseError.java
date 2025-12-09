package zw.co.dcl.jawce.engine.internal.dto;

public record ResponseError(
        String sessionId,
        String message,
        String stage
) {
}
