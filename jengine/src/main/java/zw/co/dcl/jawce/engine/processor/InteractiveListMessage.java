package zw.co.dcl.jawce.engine.processor;

import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.enums.ListSectionType;
import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.jawce.engine.processor.iface.IPayloadProcessor;
import zw.co.dcl.jawce.engine.utils.ChannelPayloadGenerator;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class InteractiveListMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final String replyMessageId;

    public InteractiveListMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        var messageBody = (Map<String, Object>) this.template.get("message");

        Map<String, Object> payload = new HashMap<>(CommonUtils.getStaticPayload(this.hookArgs.getChannelUser().waId(), PayloadType.INTERACTIVE, replyMessageId));
        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(messageBody);

        payload.put(PayloadType.INTERACTIVE.name().toLowerCase(), payloadGenerator.interactiveList());
        return payload;
    }

    @Override
    public void validator() {
        var messageBody = (Map<String, Object>) this.template.get("message");
        var sections = (LinkedHashMap<String, Object>) messageBody.get("sections");

        if (!messageBody.containsKey("body")) {
            throw new EngineInternalException("message body not found in template message");
        }

        if (!messageBody.containsKey("button")) {
            throw new EngineInternalException("List button title not found in template message");
        }

        if (CommonUtils.detectListSectionType(sections) == ListSectionType.INVALID) {
            throw new EngineInternalException("failed to parse template, possible invalid list template: [" + this.stage + "]");
        }
    }
}
