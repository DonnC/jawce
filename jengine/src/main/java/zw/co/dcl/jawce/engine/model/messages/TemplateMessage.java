package zw.co.dcl.jawce.engine.model.messages;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.AbsInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class TemplateMessage extends AbsInteractiveMessage {
    private String name;
    private String language = "en_US";
}
