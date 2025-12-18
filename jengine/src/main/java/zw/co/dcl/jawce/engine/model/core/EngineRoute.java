package zw.co.dcl.jawce.engine.model.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common trigger<br>
 * <code>"START_MENU": "re:(?i)(hi|hie|start)"</code>
 * <p>
 * Advanced trigger<br>
 * <code>
 * "TRIGGER_HELP": <br>
 *  <&nbsp> trigger: "re:^(?i)help$|(^/help)" <br>
 * <&nbsp>  route: "HELP"
 * </code>
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineRoute {
    private String userInput;
    private String nextStage;
    @Builder.Default
    private boolean isRegex = true;

    // for inner route
    private String innerNextStage;
}
