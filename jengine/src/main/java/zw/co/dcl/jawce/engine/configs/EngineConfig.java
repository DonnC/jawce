package zw.co.dcl.jawce.engine.configs;

import jakarta.annotation.Nonnull;
import lombok.Data;
import zw.co.dcl.jawce.engine.defaults.FileSessionManager;
import zw.co.dcl.jawce.engine.service.iface.IClientManager;
import zw.co.dcl.jawce.engine.service.iface.ISessionManager;
import zw.co.dcl.jawce.engine.service.iface.ITemplateStorageManager;

@Data
public class EngineConfig {
    @Nonnull
    private String startTemplateName;
    @Nonnull
    private ITemplateStorageManager storageManager;
    @Nonnull
    private IClientManager clientManager;

    // defaults
    private ISessionManager sessionManager = FileSessionManager.getInstance();
    private boolean handleSessionQueue = true;
    private boolean handleSessionInactivity = true;
    private boolean hasAuth = true;
    private boolean tagOnReply = false;
    private boolean readReceipts = false;
    private boolean logInvalidWebhooks = false;
    private int sessionTtlMins = 30;
    private long debounceTimeoutMs = 6000;
    private int webhookTimestampThresholdSecs = 10;
}
