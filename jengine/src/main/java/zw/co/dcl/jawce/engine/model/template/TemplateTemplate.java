package zw.co.dcl.jawce.engine.model.template;

import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.TemplateMessage;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateTemplate extends BaseEngineTemplate {
    private TemplateMessage message;
    private final String type = TemplateType.TEMPLATE;
}
