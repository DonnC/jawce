package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.api.enums.InteractivePayloadType;
import zw.co.dcl.jawce.engine.api.enums.PayloadType;
import zw.co.dcl.jawce.engine.api.utils.WhatsappUtils;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.ButtonMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class ButtonTemplate extends BaseEngineTemplate {
    private ButtonMessage message;

    public ButtonTemplate() {
        this.setType(TemplateType.BUTTON);
    }

}
