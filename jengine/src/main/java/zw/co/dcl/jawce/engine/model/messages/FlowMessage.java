package zw.co.dcl.jawce.engine.model.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;


@EqualsAndHashCode(callSuper = true)
@Data
public class FlowMessage extends BaseInteractiveMessage {
    @JsonProperty("flow-id")
    private String flowId;
    private boolean draft = true;
    private String name;
    private String button;
}
