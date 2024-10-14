package zw.co.dcl.jawce.chatbot.hooks

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import zw.co.dcl.jawce.engine.model.dto.HookArgsRest
import zw.co.dcl.jawce.session.ISessionManager

@Service
class UserHookService(private val sessionManager: ISessionManager) {
    private val logger = LoggerFactory.getLogger(UserHookService::class.java)

    fun captureUserRide(args: HookArgsRest): HookArgsRest {
        logger.info("RECEIVED SAVE-RIDE-DETAILS ARGS DTO: $args")

        val sessionId = args.channelUser.waId

        val session = sessionManager.session(sessionId)
        val userProps = session.getUserProps(sessionId)

        logger.info("User props: $userProps")

        // TODO: handle logic

        return args
    }
}
