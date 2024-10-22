package zw.co.dcl.jawce.engine.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody;
import zw.co.dcl.jawce.engine.model.dto.WaCurrentUser;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class DefaultHookArgs {
    private WaCurrentUser channelUser;

    private String userInput;

    // name of the flow from the template
    private String flow;

    // for rest hook return, used for dynamic routing configs
    private Map<String, Object> additionalData;

    // for flow / dynamic hooks data
    private TemplateDynamicBody templateDynamicBody;

    // if this hook call was initiated by a global trigger
    private Boolean fromTrigger = Boolean.FALSE;

    // template params
    private Map<String, Object> methodArgs;
}
