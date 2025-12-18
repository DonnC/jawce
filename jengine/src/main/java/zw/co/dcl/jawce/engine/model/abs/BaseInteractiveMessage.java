package zw.co.dcl.jawce.engine.model.abs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseInteractiveMessage extends BaseTemplateMessage {
    private String body;
    private String title;
    private String footer;
}
