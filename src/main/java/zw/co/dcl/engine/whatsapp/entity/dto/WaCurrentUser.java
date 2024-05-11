package zw.co.dcl.engine.whatsapp.entity.dto;

public record WaCurrentUser(
        String name, // display name
        String waId, // mobile number
        String msgId,  // message id
        Long timestamp // message timestamp
) {
}
