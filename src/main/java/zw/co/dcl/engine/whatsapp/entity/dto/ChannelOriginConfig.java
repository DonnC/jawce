package zw.co.dcl.engine.whatsapp.entity.dto;

import java.util.List;
import java.util.regex.Pattern;

public record ChannelOriginConfig(
        Boolean restrictOrigin,
        List<Pattern> patterns,
        boolean alertOnMismatch,
        String alertMessage,
//        can be a string (*) or a List of strings
        Object whitelistedNumbers
) {
}
