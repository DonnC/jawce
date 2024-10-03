package zw.co.dcl.jawce.chatbot.configs

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import zw.co.dcl.jawce.session.ISessionManager

@Component
class SessionLocator {
    companion object {
        var sessionManager: ISessionManager? = null
    }

    @Autowired
    fun setISessionManager(sessionConfig: SessionConfig) {
        sessionManager = sessionConfig.sessionManager()
    }
}
