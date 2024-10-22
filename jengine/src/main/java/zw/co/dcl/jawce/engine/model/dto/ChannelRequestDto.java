package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.session.ISessionManager;

public record ChannelRequestDto(
        ISessionManager session,
        MsgProcessorResponseDTO response
) {
}
