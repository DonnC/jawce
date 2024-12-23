package zw.co.dcl.jawce.engine.processor.abstracts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import zw.co.dcl.jawce.engine.constants.EngineConstants;
import zw.co.dcl.jawce.engine.exceptions.EngineInternalException;
import zw.co.dcl.jawce.engine.exceptions.EngineRenderException;
import zw.co.dcl.jawce.engine.model.DefaultHookArgs;
import zw.co.dcl.jawce.engine.model.dto.HookArgs;
import zw.co.dcl.jawce.engine.model.dto.MessageDto;
import zw.co.dcl.jawce.engine.processor.RenderProcessor;
import zw.co.dcl.jawce.engine.service.RequestService;
import zw.co.dcl.jawce.engine.utils.CommonUtils;

import java.util.Map;

public abstract class ChannelPayloadProcessor {
    private final RenderProcessor renderer = new RenderProcessor();
    private final Logger logger = LoggerFactory.getLogger(ChannelPayloadProcessor.class);
    protected String stage;
    protected Map<String, Object> template;
    protected HookArgs hookArgs;
    protected RequestService engineService;

    public ChannelPayloadProcessor(MessageDto dto) {
        this.template = dto.template();
        this.engineService = dto.engineService();
        this.hookArgs = dto.hookArgs();
        this.stage = dto.stage();
        this.validator();
        this.processTemplate();
    }

    private void processTemplate() {
        if(this.template.containsKey(EngineConstants.TPL_TEMPLATE_KEY)) {
//            dynamic templates & flows will be processed differently by the hook in DynamicMessage
            if(this.template.get("type").equals("dynamic") || this.template.get("type").equals("flow")) return;

            String hookPath = this.template.get(EngineConstants.TPL_TEMPLATE_KEY).toString();

            try {
                DefaultHookArgs result = this.engineService.processHook(hookPath, this.hookArgs);

                if(result.getTemplateDynamicBody() != null) {
                    if(result.getTemplateDynamicBody().renderPayload() != null)
                        this.template = renderer.renderTemplate(this.template, result.getTemplateDynamicBody().renderPayload());
                }
            } catch (EngineRenderException e) {
                throw e;
            } catch (Exception e) {
                logger.error("Stage: {},  failed to process template : {}", stage, e.getMessage(), e);
                throw new EngineInternalException("failed to process template call for stage: " + stage);
            }
        }
    }

    protected void validator() {
        if(!this.template.containsKey("type"))
            throw new EngineInternalException("type not found in template");
        if(!this.template.containsKey("message")) throw new EngineInternalException("message not found in tpl");
    }
}
