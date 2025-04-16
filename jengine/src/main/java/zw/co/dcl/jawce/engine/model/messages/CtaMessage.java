package zw.co.dcl.jawce.engine.model.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.AbsInteractiveMessage;


@EqualsAndHashCode(callSuper = true)
@Data
public class CtaMessage extends AbsInteractiveMessage {
    private String url;
    private String button;
}
