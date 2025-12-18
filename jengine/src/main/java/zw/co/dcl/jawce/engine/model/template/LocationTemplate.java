package zw.co.dcl.jawce.engine.model.template;


import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.LocationMessage;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LocationTemplate extends BaseEngineTemplate {
    private LocationMessage message;
    private final String type = TemplateType.LOCATION;
}
