package zw.co.dcl.jawce.engine.model.messages;

import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ButtonMessage extends BaseInteractiveMessage {
    private List<String> buttons;
    private MediaMessage header;
}
