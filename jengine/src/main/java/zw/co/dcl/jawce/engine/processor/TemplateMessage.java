package zw.co.dcl.jawce.engine.processor;

import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.model.dto.WhatsappTemplateBody;
import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.processor.iface.IPayloadProcessor;
import zw.co.dcl.jawce.engine.utils.ChannelPayloadGenerator;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Process template message as Quick messages without session handling
 * <p>
 * template: Map
 * <p>
 * {
 * "name": "template-name",
 * "language": {
 * "code": "language-and-locale-code"
 * },
 * "components": [...]
 * }
 */
public class TemplateMessage implements IPayloadProcessor {
    private final String recipient;
    private final String replyMessage;
    private final Map<String, Object> template;

    public TemplateMessage(WhatsappTemplateBody dto) {
        this.recipient = dto.recipient();
        this.template = Map.of(
                "name", dto.name(),
                "language", Map.of("code", dto.language().toString()),
                "components", dto.components()
        );
        this.replyMessage = dto.replyMessageId();
        this.validate();
    }

    public TemplateMessage(MessageDto dto) {
        this.recipient = dto.hookArgs().getWaUser().waId();
        this.template = dto.template();
        this.replyMessage = dto.replyMessageId();
        this.validate();
    }

    private void validate() {
        if (!this.template.containsKey("name") && !this.template.containsKey("components"))
            throw new EngineInternalException("Invalid template, name or components is missing");
    }

    @Override
    public Map<String, Object> generatePayload() {
        Map<String, Object> payload = new HashMap<>(
                CommonUtils.getStaticPayload(
                        recipient,
                        PayloadType.TEMPLATE,
                        replyMessage
                )
        );

        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(this.template);
        payload.put(PayloadType.TEMPLATE.name().toLowerCase(), payloadGenerator.template());

        return payload;
    }
}
