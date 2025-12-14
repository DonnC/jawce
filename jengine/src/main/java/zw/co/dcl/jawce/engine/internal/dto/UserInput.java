package zw.co.dcl.jawce.engine.internal.dto;

import java.util.Map;

public record UserInput(
        String input,
        Map<String, Object> data
) {
}
