package zw.co.dcl.jawce.engine.model.dto;

import java.util.Map;

public record ChannelUserInput(
        String input,
        Map<String, Object> additionalData
) {
}
