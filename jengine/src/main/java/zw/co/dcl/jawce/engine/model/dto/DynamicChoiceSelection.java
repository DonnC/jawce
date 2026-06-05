package zw.co.dcl.jawce.engine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicChoiceSelection implements Serializable {
    private String stage;
    private String input;
    private DynamicChoice choice;
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
}
