package zw.co.dcl.jawce.engine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import zw.co.dcl.jawce.engine.model.abs.BaseEngineTemplate;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateDynamicBody implements Serializable {
    private BaseEngineTemplate template;
    private Map<String, Object> flowPayload;
    private Map<String, Object> renderPayload;
}
