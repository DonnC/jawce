package zw.co.dcl.jawce.engine.model.dto;

public record EngineRequestSettings(
        String baseUrl,
//        is passed on every POST request to endpoint
        String authorizationToken
) {
}
