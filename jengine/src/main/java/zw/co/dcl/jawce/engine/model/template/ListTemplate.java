package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.ListMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class ListTemplate extends BaseEngineTemplate {
    private ListMessage message;

    public ListTemplate() {
        this.setType(TemplateType.LIST);
    }
}
