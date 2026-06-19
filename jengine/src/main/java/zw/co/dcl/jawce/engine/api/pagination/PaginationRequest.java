package zw.co.dcl.jawce.engine.api.pagination;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import zw.co.dcl.jawce.engine.model.dto.DynamicChoice;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationRequest {
    private String stateKey;
    private PaginationMode mode;
    @Builder.Default
    private int pageSize = 10;
    @Builder.Default
    private String prompt = "Select an option";
    @Builder.Default
    private String title = "Options";
    private String footer;
    @Builder.Default
    private String buttonLabel = "Options";
    @Builder.Default
    private String sectionTitle = "Options";
    @Builder.Default
    private String previousLabel = "Back";
    @Builder.Default
    private String nextLabel = "Next";
    @Builder.Default
    private String emptyMessage = "No items available";
    @Builder.Default
    private List<DynamicChoice> choices = List.of();
}
