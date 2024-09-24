package zw.co.dcl.jawce.engine.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.model.DefaultHookArgs;
import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.enums.PayloadType;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.processor.abstracts.ChannelPayloadProcessor;
import zw.co.dcl.jawce.engine.processor.iface.IPayloadProcessor;
import zw.co.dcl.jawce.engine.utils.ChannelPayloadGenerator;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class DynamicMessage extends ChannelPayloadProcessor implements IPayloadProcessor {
    private final Logger logger = LoggerFactory.getLogger(DynamicMessage.class);

    private final String replyMessageId;

    public DynamicMessage(MessageDto dto) {
        super(dto);
        this.replyMessageId = dto.replyMessageId();
    }

    @Override
    public Map<String, Object> generatePayload() {
        Map<String, Object> payload = new HashMap<>(
                CommonUtils.getStaticPayload(
                        this.hookArgs.getChannelUser().waId(),
                        PayloadType.INTERACTIVE,
                        replyMessageId
                )
        );

        try {
            DefaultHookArgs response = engineService.processHook(this.template.get(EngineConstants.TPL_TEMPLATE_KEY).toString(), this.hookArgs);
            TemplateDynamicBody dynamicBody = response.getTemplateDynamicBody();
            ChannelPayloadGenerator payloadGenerator = new ChannelPayloadGenerator(dynamicBody.payload());

            switch (dynamicBody.type()) {
                case TEXT -> {
                    payload.putAll(CommonUtils.getStaticPayload(this.hookArgs.getChannelUser().waId(), PayloadType.TEXT, replyMessageId));
                    payload.put("text", payloadGenerator.text());
                }
                case BUTTON -> payload.put("interactive", payloadGenerator.button());
                case INTERACTIVE -> payload.put("interactive", payloadGenerator.interactiveList());
                default ->
                        throw new EngineInternalException("failed to process dynamic template body type for stage: " + stage);
            }
        } catch (Exception err) {
            logger.error("[{}] failed to process dynamic body creation", stage, err);
            throw new EngineInternalException("failed to process dynamic template body for stage: " + stage);
        }
        return payload;
    }

    @Override
    public void validator() {
        if (!this.template.get("type").equals("dynamic")) {
            throw new EngineInternalException("template type is not set to dynamic");
        }
        if (!this.template.containsKey("template")) {
            throw new EngineInternalException("template is required for dynamic tpl message");
        }
        if (!this.template.containsKey("routes")) {
            throw new EngineInternalException("routes map is required for dynamic tpl message");
        }
    }
}
