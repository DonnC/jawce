package zw.co.dcl.jawce.engine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DynamicChoice implements Serializable {
    private String id;
    private String label;
    private String description;
    private Integer ordinal;
    @Builder.Default
    private List<String> aliases = List.of();
    @Builder.Default
    private Map<String, Object> metadata = Map.of();
}
