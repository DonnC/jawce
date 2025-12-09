package zw.co.dcl.jawce.engine.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ListSection {
    private String title;
    private List<SectionRowItem> rows;
}
