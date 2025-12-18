package zw.co.dcl.jawce.engine.model.template;


import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.CtaMessage;

@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CtaTemplate extends BaseEngineTemplate {
    private CtaMessage message;
    private final String type = TemplateType.CTA_BUTTON;

}
