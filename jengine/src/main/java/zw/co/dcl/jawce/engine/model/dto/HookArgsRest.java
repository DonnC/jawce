package zw.co.dcl.jawce.engine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import zw.co.dcl.jawce.engine.model.DefaultHookArgs;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HookArgsRest extends DefaultHookArgs {
}
