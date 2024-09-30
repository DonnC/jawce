package zw.co.dcl.jawce.chatbot.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "chatbot.configs")
data class CredentialConfigs(
    var hubToken: String? = null,
    var accessToken: String? = null,
    var phoneNumberId: String? = null,
    var sessionTtl: Int? = null,
    var initialStage: String? = null,
)
