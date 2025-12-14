package zw.co.dcl.jawce.engine.model.core;

public record FlowEndpointResponse(
        FlowEndpointPayload payload,
        byte[] aesKey,
        byte[] iv) {
}
