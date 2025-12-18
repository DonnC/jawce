package zw.co.dcl.jawce.engine.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SectionRowItem {
    private String id;
    private String title;
    private String description;
}
