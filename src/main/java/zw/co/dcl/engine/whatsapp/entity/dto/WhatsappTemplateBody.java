package zw.co.dcl.engine.whatsapp.entity.dto;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record WhatsappTemplateBody(
        String recipient,
        String name,
        Locale language,
        List<Map<String, Object>> components,
        String replyMessageId
) {
}
