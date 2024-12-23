package zw.co.dcl.jchatbot.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "channel-config")
public class ChannelConfig {
    private Boolean testLocal;
    private String localUrl;
}
