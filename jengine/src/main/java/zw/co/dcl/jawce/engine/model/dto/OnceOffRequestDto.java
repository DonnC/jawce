package zw.co.dcl.jawce.engine.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;


@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnceOffRequestDto implements Serializable {
    private String recipient;
    private Boolean validateOrigin;
    private Map<String, Object> payload;
}
