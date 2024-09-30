package zw.co.dcl.jawce.chatbot.configs

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import zw.co.dcl.jawce.session.ISessionManager
import zw.co.dcl.jawce.session.impl.FileBasedSessionManager

@Component
class SessionConfig {
    @Bean
    fun sessionManager(): ISessionManager {
        return FileBasedSessionManager.getInstance();
    }
}
