package zw.co.dcl.jawce.engine.model.messages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;


@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FlowMessage extends BaseInteractiveMessage {
    @JsonProperty("flow-id")
    private String flowId;
    private String token;
    @Builder.Default
    private boolean draft = true;
    private String name;
    private String button;
}
