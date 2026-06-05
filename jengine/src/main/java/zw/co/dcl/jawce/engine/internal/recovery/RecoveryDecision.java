package zw.co.dcl.jawce.engine.internal.recovery;

import lombok.Builder;
import lombok.Getter;
import zw.co.dcl.jawce.engine.api.dto.QuickBtnTemplate;

import java.util.Map;

@Getter
@Builder
public class RecoveryDecision {
    private final RecoveryIntent intent;
    private final QuickBtnTemplate quickButtonTemplate;
    private final boolean clearSession;
    private final boolean markRetryPending;
    private final Map<String, RecoveryIntent> recoveryActions;
}
