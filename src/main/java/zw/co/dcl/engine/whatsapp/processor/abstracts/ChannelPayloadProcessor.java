package zw.co.dcl.engine.whatsapp.processor.abstracts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.engine.whatsapp.constants.EngineConstants;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.HookArgs;
import zw.co.dcl.engine.whatsapp.entity.dto.MessageDto;
import zw.co.dcl.engine.whatsapp.exceptions.EngineInternalException;
import zw.co.dcl.engine.whatsapp.processor.ProcessorRender;
import zw.co.dcl.engine.whatsapp.service.EngineRequestService;

import java.util.Map;

public abstract class ChannelPayloadProcessor {
    private final ProcessorRender renderer = new ProcessorRender();
    private final Logger logger = LoggerFactory.getLogger(ChannelPayloadProcessor.class);
    protected String stage;
    protected Map<String, Object> template;
    protected HookArgs hookArgs;
    protected EngineRequestService engineService;

    public ChannelPayloadProcessor(MessageDto dto) {
        this.template = dto.template();
        this.engineService = dto.engineService();
        this.hookArgs = dto.hookArgs();
        this.stage = dto.stage();
        this.validator();
        this.processTemplate();
    }

    private void processTemplate() {
        if (this.template.containsKey(EngineConstants.TPL_TEMPLATE_KEY)) {
//            dynamic templates & flows will be processed differently by the hook in DynamicMessage cls
            if (this.template.get("type").equals("dynamic") || this.template.get("type").equals("flow")) return;

            String hookPath = this.template.get(EngineConstants.TPL_TEMPLATE_KEY).toString();

            try {
                DefaultHookArgs result = this.engineService.processHook(hookPath, this.hookArgs);

                if (result.getTemplateDynamicBody() != null) {
                    if (result.getTemplateDynamicBody().renderPayload() != null)
                        this.template = renderer.renderTemplate(this.template, result.getTemplateDynamicBody().renderPayload());
                }

            } catch (Exception err) {
                logger.info("[{}] [{}] failed to process template : {}", stage, hookPath, err.getMessage());
                throw new EngineInternalException("failed to process template call for stage: " + stage);
            }
        }
    }

    public void validator() {
        if (!this.template.containsKey("type"))
            throw new EngineInternalException("type not found in template");
        if (!this.template.containsKey("message")) throw new EngineInternalException("message not found in tpl");
    }
}
