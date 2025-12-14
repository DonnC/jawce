package zw.co.dcl.jawce.engine.api.dto;

import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.Hook;
import zw.co.dcl.jawce.engine.internal.service.HookService;

public record PayloadGeneratorDto(
        BaseEngineTemplate template,
        Hook hookArg,
        String stage,
        HookService hookService,
        boolean globalTagOnReply
) {
}
