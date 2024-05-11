package zw.co.dcl.engine.whatsapp.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.MessageDto;
import zw.co.dcl.engine.whatsapp.entity.dto.TemplateDynamicBody;
import zw.co.dcl.engine.whatsapp.enums.PayloadType;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.engine.whatsapp.processor.iface.IPayloadProcessor;
import zw.co.dcl.engine.whatsapp.utils.ChannelPayloadGenerator;
import zw.co.dcl.engine.whatsapp.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class FlowMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final Logger logger = LoggerFactory.getLogger(FlowMessage.class);
    private final ProcessorRender renderer = new ProcessorRender();

    private final String replyMessageId;

    public FlowMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        var messageBody = (Map<String, Object>) this.template.get("message");

        Map<String, Object> payload = new HashMap<>(CommonUtils.getStaticPayload(this.hookArgs.getChannelUser().waId(), PayloadType.INTERACTIVE, replyMessageId));
        Map<String, Object> flowData = null;

        try {
            if (this.template.containsKey("template")) {
                hookArgs.setFlow(messageBody.get("name").toString());

                DefaultHookArgs response = engineService.processHook(this.template.get(EngineConstants.TPL_TEMPLATE_KEY).toString(), this.hookArgs);
                TemplateDynamicBody dynamicBody = response.getTemplateDynamicBody();
                flowData = dynamicBody.payload();

                if (dynamicBody.renderPayload() != null) {
                    var updatedTemplate = renderer.renderTemplate(this.template, dynamicBody.renderPayload());
                    messageBody = (Map<String, Object>) updatedTemplate.get("message");
                }
            }

            ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(messageBody);

            payload.put("interactive", payloadGenerator.flow(hookArgs.getChannelUser().waId(), flowData));

        } catch (Exception err) {
            logger.error("[{}]failed to process flow body creation", stage, err);
            throw new EngineInternalException("failed to process flow template body for stage: " + stage);
        }
        return payload;
    }

    @Override
    public void validator() {
        var messageBody = (Map<String, Object>) this.template.get("message");
        boolean hasValidAttr = messageBody.containsKey("body") && messageBody.containsKey("name") && messageBody.containsKey("id") && messageBody.containsKey("button");

        if (!hasValidAttr) {
            throw new EngineInternalException("flow template is invalid");
        }
    }
}
