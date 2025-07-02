package zw.co.dcl.jawce.engine.model.abs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class BaseInteractiveMessage extends BaseTemplateMessage {
    private String body;
    private String title;
    private String footer;
}
