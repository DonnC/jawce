package zw.co.dcl.jawce.engine.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "jawce")
public class JawceConfig {
    private boolean handleSessionQueue = true;
    private boolean handleSessionInactivity = true;
    private boolean hasAuth = true;
    private boolean tagOnReply = false;
    private boolean readReceipts = false;
    private boolean logInvalidWebhooks = false;
    private int sessionTtlMins = 30;
    private long debounceTimeoutMs = 6000;
    private int webhookTimestampThresholdSecs = 10;
    private String restHookAuthToken;
    private String restHookBaseUrl;
}
