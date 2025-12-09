package zw.co.dcl.jawce.engine.model.template;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.CtaMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class CtaTemplate extends BaseEngineTemplate {
    private CtaMessage message;

    public CtaTemplate() {
        this.setType(TemplateType.CTA_BUTTON);
    }

}
