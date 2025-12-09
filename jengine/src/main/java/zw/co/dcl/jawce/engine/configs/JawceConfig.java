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
    private boolean tagOnReply = false;
    private boolean readReceipts = false;
    private boolean logInvalidWebhooks = true;
    private boolean emulate = false;
    private int sessionTtlMins = 30;
    private long debounceTimeoutMs = 3000;
    private int webhookTimestampThresholdSecs = 10;
    private String startMenu;
    private String restHookAuthToken;
    private String restHookBaseUrl;
    private String emulatorUrl = "http://localhost:3001/send-to-emulator";

    // --- global hooks

    // called after all, the webhook checks are done but before any other template processing is done
    private String onWebhookPrechecksComplete;
}
