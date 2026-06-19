package zw.co.dcl.jawce.engine.api.pagination;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaginationSelection implements Serializable {
    private String stateKey;
    private PaginationAction action;
    private Integer targetPage;
    private String choiceId;
    private String label;
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    public boolean isNavigation() {
        return action == PaginationAction.NEXT || action == PaginationAction.PREVIOUS;
    }

    public boolean isItem() {
        return action == PaginationAction.ITEM;
    }
}
