package zw.co.dcl.jawce.engine.processor;

import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.jawce.engine.processor.iface.IPayloadProcessor;
import zw.co.dcl.jawce.engine.utils.ChannelPayloadGenerator;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class CtaButtonMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final String replyMessageId;

    public CtaButtonMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        var messageBody = (Map<String, Object>) this.template.get("message");

        Map<String, Object> payload = new HashMap<>(CommonUtils.getStaticPayload(this.hookArgs.getChannelUser().waId(), PayloadType.INTERACTIVE, replyMessageId));
        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(messageBody);

        payload.put("interactive", payloadGenerator.ctaButton());
        return payload;
    }

    @Override
    public void validator() {
        var messageBody = (Map<String, Object>) this.template.get("message");

        if(!messageBody.containsKey("body")) {
            throw new EngineInternalException("message body not found in template message");
        }

        if(!messageBody.containsKey("url") || !messageBody.containsKey("button")) {
            throw new EngineInternalException("message url or button not found in CTA template message");
        }
    }
}
