package zw.co.dcl.jawce.engine.model.abs;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class AbsInteractiveMessage extends AbsTemplateMessage {
    @Nonnull
    private String body;
    private String title;
    private String footer;
}
