package zw.co.dcl.jawce.chatbot.configs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration


@Configuration
@ConfigurationProperties(prefix = "chatbot.configs")
data class ChannelCredentialConfigs(
    var hubToken: String = "",
    var accessToken: String = "",
    var phoneNumberId: String = ""
)
