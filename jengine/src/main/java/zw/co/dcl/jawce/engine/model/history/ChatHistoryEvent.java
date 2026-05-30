package zw.co.dcl.jawce.engine.model.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryEvent {
    private String timestamp;
    private HistoryEventType type;
    private String direction;
    private String sessionId;
    private String waId;
    private String messageId;
    private String stage;
    private String nextStage;
    private String templateType;
    private Boolean success;
    private String detail;
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
