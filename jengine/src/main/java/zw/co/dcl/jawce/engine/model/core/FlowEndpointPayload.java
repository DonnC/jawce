package zw.co.dcl.jawce.engine.model.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Map;

/**
 * Represents the decrypted flow payload sent by WhatsApp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlowEndpointPayload {
    private String version;
    private String action;

    private String screen;
    @JsonProperty("flow_token")
    private String flowToken;
    private Map<String, Object> data;
}
