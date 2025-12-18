package zw.co.dcl.jawce.engine.model.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.abs.BaseHook;

@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class HookRest extends BaseHook {
}
