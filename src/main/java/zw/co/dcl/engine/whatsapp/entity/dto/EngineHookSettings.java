package zw.co.dcl.engine.whatsapp.entity.dto;

public record EngineHookSettings(
        String baseUrl,
//        is passed on every POST request to endpoint
        String authorizationToken
) {
}
