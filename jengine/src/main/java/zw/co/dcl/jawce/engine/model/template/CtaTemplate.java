package zw.co.dcl.jawce.engine.model.template;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.constants.TemplateTypes;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;

@Data
@EqualsAndHashCode(callSuper = true)
public class CtaTemplate extends AbsEngineTemplate {
    private String message;

    public CtaTemplate() {
        this.setType(TemplateTypes.TEXT);
    }
}
