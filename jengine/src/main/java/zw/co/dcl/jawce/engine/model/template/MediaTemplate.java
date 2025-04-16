package zw.co.dcl.jawce.engine.model.template;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateTypes;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;
import zw.co.dcl.jawce.engine.model.messages.MediaMessage;


@Data
@EqualsAndHashCode(callSuper = true)
public class MediaTemplate extends AbsEngineTemplate {
    private MediaMessage message;

    public MediaTemplate() {
        this.setType(TemplateTypes.MEDIA);
    }
}
