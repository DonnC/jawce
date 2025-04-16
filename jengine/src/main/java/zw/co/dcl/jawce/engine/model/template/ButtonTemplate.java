package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateTypes;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class ButtonTemplate extends AbsEngineTemplate {
    private ButtonMessage message;

    public ButtonTemplate() {
        this.setType(TemplateTypes.BUTTON);
    }
}
