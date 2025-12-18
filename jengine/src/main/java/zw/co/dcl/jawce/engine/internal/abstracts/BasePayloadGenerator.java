package zw.co.dcl.jawce.engine.internal.abstracts;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import zw.co.dcl.jawce.engine.api.dto.PayloadGeneratorDto;
import zw.co.dcl.jawce.engine.api.exceptions.InternalException;
import zw.co.dcl.jawce.engine.api.exceptions.TemplateRenderException;
import zw.co.dcl.jawce.engine.api.utils.SerializeUtils;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.internal.service.RenderProcessor;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.core.Hook;

import java.util.Map;

@Slf4j
public abstract class BasePayloadGenerator {
    protected final RenderProcessor renderer = new RenderProcessor();
    protected String stage;
    protected BaseEngineTemplate template;
    protected Hook hookArg;
    protected String replyMessageId;
    protected PayloadGeneratorDto dto;

    public BasePayloadGenerator(PayloadGeneratorDto dto) {
        this.dto = dto;
        this.template = dto.template();
        this.hookArg = dto.hookArg();
        this.stage = dto.stage();
        this.replyMessageId = dto.globalTagOnReply() ? dto.hookArg().getWaUser().msgId() : dto.template().getReplyMessageId();
        this.validator();
        this.processTemplate();
    }

    Map<String, Object> processRenderTemplate() {
        try {
            this.hookArg.setHook(this.template.getTemplate());

            Assert.notNull(this.hookArg.getSession(), "hook session object is null");

            var result = this.dto.hookService().processHook(this.hookArg);

            if(result.getTemplateDynamicBody() != null) {
                if(result.getTemplateDynamicBody().getRenderPayload() != null) {
                    var renderResult = renderer.renderTemplate(SerializeUtils.fromTemplate(this.template), result.getTemplateDynamicBody().getRenderPayload());
                    this.template = SerializeUtils.toTemplate(renderResult);
                }

                return result.getTemplateDynamicBody().getFlowPayload();
            }
        } catch (TemplateRenderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Stage: {},  failed to process template : {}", stage, e.getMessage(), e);
            throw new InternalException("failed to process template call for stage: " + stage);
        }

        return null;
    }

    // the template hook will process dynamic templates & flows differently
    protected Map<String, Object> processTemplate() {
        if(this.template.getTemplate() != null) {
            if(this.template.getType().equals(TemplateType.DYNAMIC) || this.template.getType().equals(TemplateType.FLOW)) {
                return null;
            }

            return this.processRenderTemplate();
        }

        return null;
    }

    protected void validator() {
        if(this.template.getType() == null) {
            throw new InternalException("response not found in template");
        }
    }
}
