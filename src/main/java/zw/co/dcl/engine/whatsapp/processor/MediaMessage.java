package zw.co.dcl.engine.whatsapp.processor;

import zw.co.dcl.engine.whatsapp.entity.dto.MessageDto;
import zw.co.dcl.engine.whatsapp.enums.PayloadType;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.engine.whatsapp.processor.iface.IPayloadProcessor;
import zw.co.dcl.engine.whatsapp.utils.ChannelPayloadGenerator;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class MediaMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final String replyMessageId;

    public MediaMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        PayloadType type = this.template.get("type").toString().equals("document") ?
                PayloadType.DOCUMENT :
                PayloadType.IMAGE;

        Map<String, Object> payload = new HashMap<>(
                CommonUtils.getStaticPayload(
                        this.hookArgs.getChannelUser().waId(),
                        type,
                        replyMessageId
                )
        );
        var messageBody = (Map<String, Object>) this.template.get("message");

        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(messageBody);
        payload.put(type.name().toLowerCase(), payloadGenerator.media(type));
        return payload;
    }

    @Override
    public void validator() {
        var messageBody = (Map) this.template.get("message");
        boolean hasValidAttr = messageBody.containsKey("id") || messageBody.containsKey("link");
        boolean isSupportedMedia = this.template.get("type").toString().equals("document") || this.template.get("type").toString().equals("image");

        if (!hasValidAttr && !isSupportedMedia) {
            throw new EngineInternalException("Media template is invalid. Check for any of link, id or type");
        }
    }
}
