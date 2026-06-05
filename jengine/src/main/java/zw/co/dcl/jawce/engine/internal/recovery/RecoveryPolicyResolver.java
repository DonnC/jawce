package zw.co.dcl.jawce.engine.internal.recovery;

import zw.co.dcl.jawce.engine.api.dto.QuickBtnTemplate;
import zw.co.dcl.jawce.engine.api.exceptions.*;
import zw.co.dcl.jawce.engine.constants.EngineConstant;
import zw.co.dcl.jawce.engine.model.core.WaUser;

import java.util.List;
import java.util.Map;

public final class RecoveryPolicyResolver {
    private RecoveryPolicyResolver() {
    }

    public static RecoveryDecision resolve(WaUser user, Exception exception) {
        if(exception instanceof SessionExpiredException || exception instanceof SessionInactivityException) {
            return RecoveryDecision.builder()
                    .intent(RecoveryIntent.RESTART)
                    .clearSession(true)
                    .quickButtonTemplate(QuickBtnTemplate.builder()
                            .recipient(user.waId())
                            .messageId(user.msgId())
                            .title("Security Check ðŸ”")
                            .footer("Session Expired")
                            .buttons(List.of(EngineConstant.BTN_MENU))
                            .message(exception.getMessage())
                            .build())
                    .recoveryActions(Map.of(EngineConstant.BTN_MENU, RecoveryIntent.RESTART))
                    .build();
        }

        if(exception instanceof HookException) {
            return RecoveryDecision.builder()
                    .intent(RecoveryIntent.RETRY)
                    .markRetryPending(true)
                    .quickButtonTemplate(QuickBtnTemplate.builder()
                            .recipient(user.waId())
                            .messageId(user.msgId())
                            .title("Message")
                            .buttons(List.of(EngineConstant.BTN_RETRY))
                            .message(exception.getMessage())
                            .build())
                    .build();
        }

        if(exception instanceof TemplateRenderException) {
            return RecoveryDecision.builder()
                    .intent(RecoveryIntent.RETRY)
                    .markRetryPending(true)
                    .quickButtonTemplate(QuickBtnTemplate.builder()
                            .recipient(user.waId())
                            .messageId(user.msgId())
                            .title("Message")
                            .buttons(List.of(EngineConstant.BTN_RETRY))
                            .message("Failed to process your message")
                            .build())
                    .build();
        }

        if(exception instanceof ResponseException responseException) {
            return RecoveryDecision.builder()
                    .intent(RecoveryIntent.RETURN_TO_MENU)
                    .quickButtonTemplate(QuickBtnTemplate.builder()
                            .recipient(user.waId())
                            .messageId(user.msgId())
                            .title("Message")
                            .buttons(List.of(EngineConstant.BTN_MENU))
                            .message("%s.\n\n%s".formatted(
                                    responseException.getError().message(),
                                    "You may click the button to return to Menu"
                            ))
                            .build())
                    .recoveryActions(Map.of(EngineConstant.BTN_MENU, RecoveryIntent.RETURN_TO_MENU))
                    .build();
        }

        if(exception instanceof UserSessionValidationException) {
            return RecoveryDecision.builder()
                    .intent(RecoveryIntent.RETURN_TO_MENU)
                    .quickButtonTemplate(QuickBtnTemplate.builder()
                            .recipient(user.waId())
                            .messageId(user.msgId())
                            .title("Message")
                            .buttons(List.of(EngineConstant.BTN_MENU))
                            .message("Could not process request\n\n_AMB Err_")
                            .build())
                    .recoveryActions(Map.of(EngineConstant.BTN_MENU, RecoveryIntent.RETURN_TO_MENU))
                    .build();
        }

        return RecoveryDecision.builder()
                .intent(RecoveryIntent.RETURN_TO_MENU)
                .quickButtonTemplate(QuickBtnTemplate.builder()
                        .recipient(user.waId())
                        .messageId(user.msgId())
                        .title("Message")
                        .buttons(List.of(EngineConstant.BTN_MENU))
                        .message("Something went wrong. Please try again later.")
                        .build())
                .recoveryActions(Map.of(EngineConstant.BTN_MENU, RecoveryIntent.RETURN_TO_MENU))
                .build();
    }
}
