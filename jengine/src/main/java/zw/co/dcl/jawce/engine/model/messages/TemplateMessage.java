package zw.co.dcl.jawce.engine.model.messages;


import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
public class TemplateMessage extends BaseInteractiveMessage {
    private String name;
    private String language = "en_US";
}
