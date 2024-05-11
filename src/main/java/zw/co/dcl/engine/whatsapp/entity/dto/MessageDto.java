package zw.co.dcl.engine.whatsapp.entity.dto;

import zw.co.dcl.engine.whatsapp.service.EngineRequestService;

import java.util.Map;

public record MessageDto(
        EngineRequestService engineService,
        Map<String, Object> template,
        HookArgs hookArgs,
        String stage,
        String replyMessageId
) {
}
