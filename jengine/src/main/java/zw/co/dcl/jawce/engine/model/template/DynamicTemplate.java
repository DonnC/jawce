package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DynamicTemplate extends BaseEngineTemplate {
    private String message;

    public DynamicTemplate() {
        this.setType(TemplateType.DYNAMIC);
    }
}
