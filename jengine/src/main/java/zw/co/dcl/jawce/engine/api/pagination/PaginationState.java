package zw.co.dcl.jawce.engine.api.pagination;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaginationState implements Serializable {
    private String stateKey;
    private PaginationMode mode;
    private Integer page;
    private Integer pageSize;
    private Integer totalItems;
    private Integer totalPages;
}
