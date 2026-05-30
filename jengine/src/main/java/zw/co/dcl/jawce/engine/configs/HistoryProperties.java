package zw.co.dcl.jawce.engine.configs;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jawce.history")
public class HistoryProperties {
    /**
     * Enables the bundled file-based history manager. When false the engine
     * falls back to a no-op manager unless the host app supplies its own bean.
     */
    private boolean fileEnabled = false;
    private String dir = "./.history";
    private String filePrefix = "jawce-history";
    private long maxFileSizeBytes = 5 * 1024 * 1024;
    private int maxFiles = 20;
    private int executorThreads = 1;
}
