package zw.co.dcl.engine.whatsapp.entity.dto;

import java.util.Map;

public record ChannelUserInput(
        String input,
        Map<String, Object> additionalData
) {
}
