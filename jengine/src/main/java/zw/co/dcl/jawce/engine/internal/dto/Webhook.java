package zw.co.dcl.jawce.engine.internal.dto;

import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.ResponseStructure;

import java.util.Map;

public record Webhook(
        WaUser user,
        ResponseStructure response
) {
}
