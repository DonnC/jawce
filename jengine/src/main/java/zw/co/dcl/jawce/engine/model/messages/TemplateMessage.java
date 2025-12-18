package zw.co.dcl.jawce.engine.model.messages;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.abs.BaseInteractiveMessage;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateMessage extends BaseInteractiveMessage {
    private String name;
    @Builder.Default
    private String language = "en_US";
}
