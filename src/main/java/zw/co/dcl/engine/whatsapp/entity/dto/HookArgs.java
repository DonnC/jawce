package zw.co.dcl.engine.whatsapp.entity.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import zw.co.dcl.engine.whatsapp.entity.DefaultHookArgs;
import zw.co.dcl.engine.whatsapp.service.iface.ISessionManager;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HookArgs extends DefaultHookArgs {
    //    set to null when sending via rest call
    private ISessionManager session;
}
