package zw.co.dcl.engine.whatsapp.entity.dto;

import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;

public record ChannelRequestDto(
        ISessionManager session,
        MsgProcessorResponseDTO response
) {
}
