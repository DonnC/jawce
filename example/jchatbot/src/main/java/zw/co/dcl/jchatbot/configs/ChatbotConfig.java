package zw.co.dcl.jchatbot.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "chatbot.configs")
public class ChatbotConfig {
    private String cache;
    private String initialStage;
    private int sessionTtl;
    private String hubToken;
    private String phoneNumberId;
    private String accessToken;
}
