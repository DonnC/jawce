package zw.co.dcl.jawce.engine.model.template;

import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;


@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLocationTemplate extends BaseEngineTemplate {
    private String message;
    private final String type = TemplateType.REQUEST_LOCATION;
}
