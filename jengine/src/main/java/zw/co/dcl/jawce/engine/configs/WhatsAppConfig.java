package zw.co.dcl.jawce.engine.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppConfig {
    private String hubToken;
    private String accessToken;
    private String phoneNumberId;
    private String appSecret;
    private String apiVersion = "v24.0";
}
