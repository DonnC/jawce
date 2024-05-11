package zw.co.dcl.engine.whatsapp.entity.dto;

import java.util.List;

public record QuickBtnPayload(
        String recipient,
        String message,
        String footer,
        String title,
        List<String> buttons,
        String replyMessageId
) {
}
