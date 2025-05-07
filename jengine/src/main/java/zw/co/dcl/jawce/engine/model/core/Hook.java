package zw.co.dcl.jawce.engine.model.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import zw.co.dcl.jawce.engine.model.abs.BaseHook;
import zw.co.dcl.jawce.engine.api.iface.ISessionManager;


@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Hook extends BaseHook {
    private ISessionManager session;
}
