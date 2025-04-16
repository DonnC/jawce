package zw.co.dcl.jawce.engine.processor;

import lombok.extern.slf4j.Slf4j;
import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.jawce.engine.processor.iface.IPayloadProcessor;
import zw.co.dcl.jawce.engine.utils.ChannelPayloadGenerator;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class MediaMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final String replyMessageId;

    public MediaMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        Map<String, Object> payload = new HashMap<>(
                CommonUtils.getStaticPayload(
                        this.hookArgs.getWaUser().waId(),
                        PayloadType.MEDIA,
                        replyMessageId
                )
        );
        var messageBody = (Map<String, Object>) this.template.get("message");
        String mediaType = messageBody.get("type").toString();

        payload.put("type", mediaType);

        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(messageBody);
        payload.put(mediaType, payloadGenerator.media());

        log.info("{} MEDIA PAYLOAD: {}", mediaType, payload);

        return payload;
    }

    @Override
    public void validator() {
        var messageBody = (Map) this.template.get("message");
        boolean hasValidAttr = this.template.get("type").equals("media") && messageBody.containsKey("type") && (messageBody.containsKey("id") || messageBody.containsKey("link"));

        if (!hasValidAttr) {
            throw new EngineInternalException("Media template is invalid. Check for any of link, id or type");
        }
    }
}
