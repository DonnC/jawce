package zw.co.dcl.jawce.engine.model.template;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.FlowMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTemplate extends BaseEngineTemplate {
    private FlowMessage message;

    public FlowTemplate() {
        this.setType(TemplateType.FLOW);
    }
}
