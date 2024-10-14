package zw.co.dcl.jawce.chatbot.hooks

import zw.co.dcl.jawce.chatbot.configs.SessionLocator
import zw.co.dcl.jawce.engine.enums.WebhookResponseMessageType
import zw.co.dcl.jawce.engine.model.dto.HookArgs
import zw.co.dcl.jawce.engine.model.dto.TemplateDynamicBody
import zw.co.dcl.jawce.session.ISessionManager

// UserHook being used via reflection API
class ReflUserHook {
    private val args: HookArgs
    private val session: ISessionManager?
    private val sessionId: String

    constructor(args: HookArgs) {
        this.args = args
        sessionId = args.channelUser.waId
        session = SessionLocator.sessionManager?.session(sessionId)
    }

    fun getDefaultUsername(): HookArgs {
        args.templateDynamicBody = TemplateDynamicBody(
            WebhookResponseMessageType.BUTTON,
            null,
            mapOf("user" to args.channelUser.name)
        )

        return args
    }
}
