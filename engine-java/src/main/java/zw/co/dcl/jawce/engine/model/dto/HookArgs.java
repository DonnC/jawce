package zw.co.dcl.jawce.engine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import zw.co.dcl.jawce.engine.model.DefaultHookArgs;
import zw.co.dcl.jawce.session.ISessionManager;


@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HookArgs extends DefaultHookArgs {
    //    set to null when sending via rest call
    private ISessionManager session;
}
