package zw.co.dcl.jawce.engine.model.abs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.core.WaUser;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public abstract class BaseHook implements Serializable {
    private WaUser waUser;
    private String sessionId;
    private String userInput;
    private String flow;
    private Map<String, Object> additionalData;
    private TemplateDynamicBody templateDynamicBody;
    @Builder.Default
    private boolean fromTrigger = false;
    private String hook;
    private String redirectTo;
    @Builder.Default
    private Map<String, Object> params = new HashMap<>();
}
