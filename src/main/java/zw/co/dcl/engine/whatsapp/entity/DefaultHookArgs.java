package zw.co.dcl.engine.whatsapp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import zw.co.dcl.engine.whatsapp.entity.dto.TemplateDynamicBody;
import zw.co.dcl.engine.whatsapp.entity.dto.WaCurrentUser;

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
    //        name of the flow from the template
    private String flow;
    //  for rest hook return, set any data here to save to session
    private Map<String, Object> additionalData;
    //    for flow / dynamic hooks data
    private TemplateDynamicBody templateDynamicBody;
    //  pass tpl defined args
    private Map<String, Object> methodArgs;
}
