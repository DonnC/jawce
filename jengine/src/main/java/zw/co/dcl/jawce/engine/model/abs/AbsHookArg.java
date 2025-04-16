package zw.co.dcl.jawce.engine.model.abs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbsHookArg {
    private WaUser waUser;
    private String sessionId;
    private String userInput;
    private String flow;
    private Map<String, Object> additionalData;
    private TemplateDynamicBody templateDynamicBody;
    private boolean fromTrigger = false;
    private boolean hasAuth = false;
    private String hook;
    private Map<String, Object> params;
}
