package zw.co.dcl.jawce.engine.model.template;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.LocationMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class LocationTemplate extends BaseEngineTemplate {
    private LocationMessage message;

    public LocationTemplate() {
        this.setType(TemplateType.LOCATION);
    }
}
