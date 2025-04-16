package zw.co.dcl.jawce.engine.model.dto;

import lombok.Data;
import zw.co.dcl.jawce.engine.model.abs.AbsEngineTemplate;

import java.util.Map;

@Data
public class TemplateDynamicBody {
    private AbsEngineTemplate template;
    private Map<String, Object> flowPayload;
    private Map<String, Object> renderPayload;
}
