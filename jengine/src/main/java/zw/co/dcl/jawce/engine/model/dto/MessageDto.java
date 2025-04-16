package zw.co.dcl.jawce.engine.model.dto;

import zw.co.dcl.jawce.engine.model.core.HookArg;
import zw.co.dcl.jawce.engine.service.RequestService;

import java.util.Map;

public record MessageDto(
        RequestService engineService,
        Map<String, Object> template,
        HookArg hookArgs,
        String stage,
        String replyMessageId
) {
}
