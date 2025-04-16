package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateTypes;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.TemplateMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateTemplate extends AbsEngineTemplate {
    private TemplateMessage message;

    public TemplateTemplate() {
        this.setType(TemplateTypes.TEMPLATE);
    }
}
