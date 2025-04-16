package zw.co.dcl.jawce.engine.model.template;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateTypes;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.LocationMessage;

@Data
@EqualsAndHashCode(callSuper = true)
public class LocationTemplate extends AbsEngineTemplate {
    private LocationMessage message;

    public LocationTemplate() {
        this.setType(TemplateTypes.LOCATION);
    }
}
