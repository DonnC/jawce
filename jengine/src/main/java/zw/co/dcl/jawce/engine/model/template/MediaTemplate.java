package zw.co.dcl.jawce.engine.model.template;

import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.MediaMessage;


@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class MediaTemplate extends BaseEngineTemplate {
    private MediaMessage message;
    private final String type = TemplateType.MEDIA;
}
