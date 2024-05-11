package zw.co.dcl.engine.whatsapp.processor;

import zw.co.dcl.engine.whatsapp.enums.PayloadType;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.processor.iface.IPayloadProcessor;
import zw.co.dcl.engine.whatsapp.utils.ChannelPayloadGenerator;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class ReactionMessage implements IPayloadProcessor {
    private final String recipient;
    private final Map<String, Object> template;

    public ReactionMessage(String recipient, Map<String, Object> template) {
        this.recipient = recipient;
        this.template = template;
        this.validate();
    }

    private void validate() {
        if (!this.template.containsKey("message_id")) throw new EngineInternalException("Invalid Reaction message");
    }

    @Override
    public Map<String, Object> generatePayload() {
        Map<String, Object> payload = new HashMap<>(
                CommonUtils.getStaticPayload(
                        recipient,
                        PayloadType.REACTION,
                        null
                )
        );

        ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(this.template);
        payload.put(PayloadType.REACTION.name().toLowerCase(), payloadGenerator.reaction());

        return payload;
    }
}
