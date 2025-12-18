package zw.co.dcl.jawce.engine.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListSection {
    private String title;
    private List<SectionRowItem> rows;
}
