package zw.co.dcl.jawce.engine.model.core;

import lombok.*;
import lombok.experimental.SuperBuilder;
import zw.co.dcl.jawce.engine.model.abs.BaseHook;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Hook extends BaseHook {
    private ISessionManager session;
}
