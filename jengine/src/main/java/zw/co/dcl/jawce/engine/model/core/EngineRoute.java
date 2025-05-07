package zw.co.dcl.jawce.engine.model.core;

import lombok.Data;

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
public class EngineRoute {
    private String userInput;
    private String nextStage;
    private boolean isRegex = true;

    // for inner route
    private String innerNextStage;
}
