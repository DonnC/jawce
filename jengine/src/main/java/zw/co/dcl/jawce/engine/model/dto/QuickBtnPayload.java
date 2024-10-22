package zw.co.dcl.jawce.engine.model.dto;

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
