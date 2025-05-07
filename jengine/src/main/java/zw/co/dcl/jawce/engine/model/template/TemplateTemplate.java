package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.TemplateMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateTemplate extends BaseEngineTemplate {
    private TemplateMessage message;

    public TemplateTemplate() {
        this.setType(TemplateType.TEMPLATE);
    }
}
