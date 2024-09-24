package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.service.EngineRequestService;

import java.util.Map;

public record MessageDto(
        EngineRequestService engineService,
        Map<String, Object> template,
        HookArgs hookArgs,
        String stage,
        String replyMessageId
) {
}
