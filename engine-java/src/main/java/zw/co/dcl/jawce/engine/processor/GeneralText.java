package zw.co.dcl.jawce.engine.processor;

import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.jawce.engine.processor.iface.IPayloadProcessor;
import zw.co.dcl.jawce.engine.utils.ChannelPayloadGenerator;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class GeneralText extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final String replyMessageId;

    public GeneralText(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        Map<String, Object> payload = new HashMap<>(
                CommonUtils.getStaticPayload(
                        this.hookArgs.getChannelUser().waId(),
                        PayloadType.TEXT,
                        replyMessageId
                )
        );

        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(this.template);
        payload.put(PayloadType.TEXT.name().toLowerCase(), payloadGenerator.text());

        return payload;
    }
}
