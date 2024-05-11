package zw.co.dcl.engine.whatsapp.processor;

import zw.co.dcl.engine.whatsapp.entity.dto.MessageDto;
import zw.co.dcl.engine.whatsapp.enums.PayloadType;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.engine.whatsapp.processor.iface.IPayloadProcessor;
import zw.co.dcl.engine.whatsapp.utils.ChannelPayloadGenerator;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ButtonMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final String replyMessageId;

    public ButtonMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        var messageBody = (Map<String, Object>) this.template.get("message");

        Map<String, Object> payload = new HashMap<>(CommonUtils.getStaticPayload(this.hookArgs.getChannelUser().waId(), PayloadType.INTERACTIVE, replyMessageId));
        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(messageBody);

        payload.put("interactive", payloadGenerator.button());
        return payload;
    }

    @Override
    public void validator() {
        var messageBody = (Map<String, Object>) this.template.get("message");
        var buttonList = (ArrayList<String>) messageBody.get("buttons");

        if (!messageBody.containsKey("body")) {
            throw new EngineInternalException("message body not found in template message");
        }

        if (buttonList.size() > 3) {
            throw new EngineInternalException("too many buttons options in template message");
        }
    }
}
