package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class ButtonTemplate extends BaseEngineTemplate {
    private ButtonMessage message;

    public ButtonTemplate() {
        this.setType(TemplateType.BUTTON);
    }
}
