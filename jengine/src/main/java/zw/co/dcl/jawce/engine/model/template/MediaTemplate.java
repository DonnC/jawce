package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateType;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.MediaMessage;


@Data
@EqualsAndHashCode(callSuper = true)
public class MediaTemplate extends BaseEngineTemplate {
    private MediaMessage message;

    public MediaTemplate() {
        this.setType(TemplateType.MEDIA);
    }
}
