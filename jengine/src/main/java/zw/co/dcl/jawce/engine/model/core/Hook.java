package zw.co.dcl.jawce.engine.model.core;

import lombok.*;
import zw.co.dcl.jawce.engine.model.abs.BaseHook;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hook extends BaseHook {
    private ISessionManager session;
}
