package zw.co.dcl.engine.whatsapp.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HookArgsRest extends DefaultHookArgs {
}
