package zw.co.dcl.jawce.engine.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionRowItem {
    private String id;
    private String title;
    private String description;
}
