package zw.co.dcl.jawce.engine.model.template;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateTypes;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.FlowMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class FlowTemplate extends AbsEngineTemplate {
    private FlowMessage message;

    public FlowTemplate() {
        this.setType(TemplateTypes.FLOW);
    }
}
