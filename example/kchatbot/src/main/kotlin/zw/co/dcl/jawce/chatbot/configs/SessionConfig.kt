package zw.co.dcl.jawce.chatbot.configs

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import zw.co.dcl.jawce.session.ISessionManager
import zw.co.dcl.jawce.session.impl.CaffeineSessionManager
import zw.co.dcl.jawce.session.impl.FileBasedSessionManager
import java.util.concurrent.TimeUnit

@Component
class SessionConfig(private val configs: CredentialConfigs) {
    @Bean
    fun sessionManager(): ISessionManager {
        return when (configs.cache) {
            "caffeine" -> CaffeineSessionManager.getInstance(30, TimeUnit.MINUTES)
            else -> FileBasedSessionManager.getInstance()
        }
    }
}
